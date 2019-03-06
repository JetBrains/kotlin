/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.impl.FirClassImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl
import org.jetbrains.kotlin.fir.deserialization.FirTypeDeserializer
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.getOrPut
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.supertypes
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.ProtoBasedClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import java.io.InputStream

class FirLibrarySymbolProviderImpl(val session: FirSession) : FirSymbolProvider {
    override fun getCallableSymbols(callableId: CallableId): List<ConeCallableSymbol> {
        // TODO
        return emptyList()
    }

    private class BuiltInsPackageFragment(stream: InputStream, val fqName: FqName, val session: FirSession) {
        lateinit var version: BuiltInsBinaryVersion

        val packageProto: ProtoBuf.PackageFragment = run {

            version = BuiltInsBinaryVersion.readFrom(stream)

            if (!version.isCompatible()) {
                // TODO: report a proper diagnostic
                throw UnsupportedOperationException(
                    "Kotlin built-in definition format version is not supported: " +
                            "expected ${BuiltInsBinaryVersion.INSTANCE}, actual $version. " +
                            "Please update Kotlin"
                )
            }

            ProtoBuf.PackageFragment.parseFrom(stream, BuiltInSerializerProtocol.extensionRegistry)
        }

        private val nameResolver = NameResolverImpl(packageProto.strings, packageProto.qualifiedNames)

        val classDataFinder = ProtoBasedClassDataFinder(packageProto, nameResolver, version) { SourceElement.NO_SOURCE }


        val lookup = mutableMapOf<ClassId, ConeClassLikeSymbol>()

        private fun createTypeParameterSymbol(name: Name): FirTypeParameterSymbol {
            val firSymbol = FirTypeParameterSymbol()
            FirTypeParameterImpl(session, null, firSymbol, name, variance = Variance.INVARIANT, isReified = false)
            return firSymbol
        }

        fun getClassLikeSymbolByFqName(classId: ClassId, provider: FirSymbolProvider): ConeClassLikeSymbol? {

            if (classId !in classDataFinder.allClassIds) return null
            return lookup.getOrPut(classId, { FirClassSymbol(classId) }) { symbol ->
                val classData = classDataFinder.findClassData(classId)!!
                val classProto = classData.classProto
                val flags = classProto.flags
                val kind = Flags.CLASS_KIND.get(flags)
                FirClassImpl(
                    session, null, symbol, classId.shortClassName,
                    ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags)),
                    ProtoEnumFlags.modality(Flags.MODALITY.get(flags)),
                    Flags.IS_EXPECT_CLASS.get(flags), false,
                    ProtoEnumFlags.classKind(kind),
                    Flags.IS_INNER.get(flags),
                    kind == ProtoBuf.Class.Kind.COMPANION_OBJECT,
                    Flags.IS_DATA.get(classProto.flags),
                    Flags.IS_INLINE_CLASS.get(classProto.flags)
                ).apply {
                    for (typeParameter in classProto.typeParameterList) {
                        typeParameters += createTypeParameterSymbol(classData.nameResolver.getName(typeParameter.name)).fir
                    }
                    //addAnnotationsFrom(classProto) ? TODO

                    val typeTable = TypeTable(classData.classProto.typeTable)
                    val typeDeserializer = FirTypeDeserializer(
                        classData.nameResolver,
                        typeTable,
                        provider,
                        classData.classProto.typeParameterList,
                        null
                    )


                    val superTypesDeserialized = classProto.supertypes(typeTable).map { supertypeProto ->
                        typeDeserializer.classLikeType(supertypeProto)
                    }// TODO: + c.components.additionalClassPartsProvider.getSupertypes(this@DeserializedClassDescriptor)

                    superTypesDeserialized.mapNotNullTo(superTypeRefs) {
                        if (it == null) return@mapNotNullTo null
                        FirResolvedTypeRefImpl(this@BuiltInsPackageFragment.session, null, it, false, emptyList())
                    }
                    // TODO: declarations (probably should be done later)
                }
            }
        }
    }

    override fun getPackage(fqName: FqName): FqName? {
        if (allPackageFragments.containsKey(fqName)) return fqName
        return null
    }

    private fun loadBuiltIns(): List<BuiltInsPackageFragment> {
        val classLoader = this::class.java.classLoader
        val streamProvider = { path: String -> classLoader?.getResourceAsStream(path) ?: ClassLoader.getSystemResourceAsStream(path) }
        val packageFqNames = KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAMES

        return packageFqNames.map { fqName ->
            val resourcePath = BuiltInSerializerProtocol.getBuiltInsFilePath(fqName)
            val inputStream = streamProvider(resourcePath) ?: throw IllegalStateException("Resource not found in classpath: $resourcePath")
            BuiltInsPackageFragment(inputStream, fqName, session)
        }
    }

    private val allPackageFragments = loadBuiltIns().groupBy { it.fqName }

    private val fictitiousFunctionSymbols = mutableMapOf<Int, ConeClassSymbol>()

    override fun getClassLikeSymbolByFqName(classId: ClassId): ConeClassLikeSymbol? {
        return allPackageFragments[classId.packageFqName]?.firstNotNullResult {
            it.getClassLikeSymbolByFqName(classId, this)
        } ?: with(classId) {
            val className = relativeClassName.asString()
            val kind = FunctionClassDescriptor.Kind.byClassNamePrefix(packageFqName, className) ?: return@with null
            val prefix = kind.classNamePrefix
            val arity = className.substring(prefix.length).toIntOrNull() ?: return null
            fictitiousFunctionSymbols.getOrPut(arity) {
                FirClassSymbol(this).apply {
                    FirClassImpl(
                        session,
                        null,
                        this,
                        relativeClassName.shortName(),
                        Visibilities.PUBLIC,
                        Modality.OPEN,
                        false,
                        false,
                        ClassKind.CLASS,
                        isInner = false,
                        isCompanion = false,
                        isData = false,
                        isInline = false
                    )
                }
            }
        }
    }
}