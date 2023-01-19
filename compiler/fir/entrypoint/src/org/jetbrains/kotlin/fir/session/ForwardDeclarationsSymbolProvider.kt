/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.BinaryModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.getDeprecationsProvider
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassType
import org.jetbrains.kotlin.library.exportForwardDeclarations
import org.jetbrains.kotlin.library.isInterop
import org.jetbrains.kotlin.library.metadata.impl.ExportedForwardDeclarationChecker
import org.jetbrains.kotlin.library.metadata.impl.ForwardDeclarationsFqNames
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.library.packageFqName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.SmartList

class ForwardDeclarationsSymbolProvider(
    session: FirSession,
    moduleDataProvider: ModuleDataProvider,
    private val kotlinScopeProvider: FirKotlinScopeProvider,
    val delegate: KlibBasedSymbolProvider,
    private val resolvedLibraries: Collection<KotlinResolvedLibrary>,
) : FirSymbolProvider(session) {

    private class InteropLibraryInfo(
        val library: KotlinResolvedLibrary,
        val mainPackageName: FqName,
        val exportedForwardDeclarationsByName: Map<String, FqName>,
    )

    private val forwardDeclarationsModuleData = BinaryModuleData.createDependencyModuleData(
        Name.special("<forward declarations>"),
        moduleDataProvider.platform,
        moduleDataProvider.analyzerServices,
    ).apply {
        bindSession(session)
    }

    private val interopLibrariesByPackageName: Map<String, List<InteropLibraryInfo>> by lazy {
        // FIXME: more granularly lazy?
        buildMap<String, SmartList<InteropLibraryInfo>> {
            for (resolvedLibrary in resolvedLibraries) {
                val library = resolvedLibrary.library
                if (!library.isInterop) continue

                val mainPackageFqName = library.packageFqName?. let{ FqName(it) }
                    ?: error("Inconsistent manifest: interop library ${library.libraryName} should have `package` specified")
                val exportedForwardDeclarations = library.exportForwardDeclarations.map { FqName(it) }.associateBy { it.shortName().asString() }

                getOrPut(mainPackageFqName.asString() /* FIXME */) { SmartList() }.add(
                    InteropLibraryInfo(
                        resolvedLibrary, mainPackageFqName, exportedForwardDeclarations
                    )
                )
            }
        }
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        val packageStringName = classId.packageFqName.asString()

        if (packageStringName in ExportedForwardDeclarationChecker.values().map { it.fqName.asString() }) {
            // FIXME: this would probably lead to class being deserialized twice (with two different symbols).
            return getForwardDeclarationClassLikeSymbolByClassId(classId)
        }

        for (interopLibraryInfo in interopLibrariesByPackageName[packageStringName].orEmpty()) {
            val exportedForwardDeclarationPackage =
                interopLibraryInfo.exportedForwardDeclarationsByName[classId.outermostClassId.shortClassName.asString()]?.parent() ?: continue

            val exportedForwardDeclarationClassId = ClassId(exportedForwardDeclarationPackage, classId.relativeClassName, classId.isLocal)

            // FIXME: can directly use specialized *cnames.* impl.
            return getClassLikeSymbolByClassId(exportedForwardDeclarationClassId)
        }

        return null
    }

    private fun getForwardDeclarationClassLikeSymbolByClassId(
        classId: ClassId
    ): FirClassLikeSymbol<*>? {
        val packageStringName = classId.packageFqName.asString()
//        val checker = ExportedForwardDeclarationChecker.values().single { it.fqName.asString() == packageStringName }
        interopLibrariesByPackageName.values.flatten().forEach { interopLibraryInfo ->
            // FIXME: check if the library has this classifier first, by using classifierNames.
            // FIXME: use only this specific library
            // FIXME: check kind and supertype
            val newClassId = ClassId(interopLibraryInfo.mainPackageName, classId.relativeClassName, classId.isLocal)
            delegate.getClassLikeSymbolByClassId(newClassId)?.let {
                return it
            }
        }

        val superClassFqName: String
        val classKind: ClassKind

        when (FqName(packageStringName)) {
            ForwardDeclarationsFqNames.cNamesStructs -> {
                classKind = ClassKind.CLASS
                superClassFqName = "kotlinx.cinterop.COpaque"
            }
            ForwardDeclarationsFqNames.objCNamesClasses -> {
                classKind = ClassKind.CLASS
                superClassFqName = "kotlinx.cinterop.ObjCObjectBase"
            }
            ForwardDeclarationsFqNames.objCNamesProtocols -> {
                classKind = ClassKind.INTERFACE
                superClassFqName = "kotlinx.cinterop.ObjCObject"
            }
            else -> error(classId)
        }

        if (classId.isNestedClass) return null

        val symbol = FirRegularClassSymbol(classId)
        val modality = Modality.FINAL
        val visibility = Visibilities.Public
        val status = FirResolvedDeclarationStatusImpl(
            visibility,
            modality,
            EffectiveVisibility.Public
        ).apply {
            isExpect = false // FIXME: copy from descriptors
            isActual = false
            isCompanion = false
            isInner = false
            isData = false
            isInline = false
            isExternal = false
            isFun = false
        }

        buildRegularClass {
            this.moduleData = forwardDeclarationsModuleData
            this.origin = FirDeclarationOrigin.Library
            name = classId.shortClassName
            this.status = status
            this.classKind = classKind
            this.scopeProvider = kotlinScopeProvider // FIXME
            this.symbol = symbol

            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES

            superTypeRefs += buildResolvedTypeRef {
                type = ConeClassLikeLookupTagImpl(ClassId.topLevel(FqName(superClassFqName)))
                    .constructClassType(emptyArray(), isNullable = false)
            }

        }.apply {
//                versionRequirementsTable = context.versionRequirementTable

//                sourceElement = containerSource

            replaceDeprecationsProvider(getDeprecationsProvider(session))

//                classProto.getExtensionOrNull(JvmProtoBuf.classModuleName)?.let { idx ->
//                    moduleName = nameResolver.getString(idx)
//                }
        }
        return symbol
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
    }

    override fun getPackage(fqName: FqName): FqName? {
        if (fqName.asString() in interopLibrariesByPackageName) return fqName
        if (fqName in setOf(ForwardDeclarationsFqNames.cNamesStructs, ForwardDeclarationsFqNames.objCNamesClasses, ForwardDeclarationsFqNames.objCNamesProtocols)) {
            return fqName
        }
        return null
    }
}