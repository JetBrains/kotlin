/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirNamedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirEnumEntryImpl
import org.jetbrains.kotlin.fir.deserialization.FirDeserializationContext
import org.jetbrains.kotlin.fir.deserialization.deserializeClassToSymbol
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirClassReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.java.createConstant
import org.jetbrains.kotlin.fir.java.topLevelName
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmNameResolver
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ClassLiteralValue
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class KotlinDeserializedJvmSymbolsProvider(
    val session: FirSession,
    val project: Project,
    private val packagePartProvider: PackagePartProvider,
    private val kotlinClassFinder: KotlinClassFinder,
    private val javaClassFinder: JavaClassFinder
) : AbstractFirSymbolProvider() {

    private val classesCache = mutableMapOf<ClassId, FirClassSymbol>()
    private val typeAliasCache = mutableMapOf<ClassId, FirTypeAliasSymbol?>()
    private val packagePartsCache = mutableMapOf<FqName, Collection<PackagePartsCacheData>>()

    private class PackagePartsCacheData(val proto: ProtoBuf.Package, val context: FirDeserializationContext) {
        val topLevelFunctionNameIndex by lazy {
            proto.functionList.withIndex()
                .groupBy({ context.nameResolver.getName(it.value.name) }) { (index) -> index }
        }
        val topLevelPropertyNameIndex by lazy {
            proto.propertyList.withIndex()
                .groupBy({ context.nameResolver.getName(it.value.name) }) { (index) -> index }
        }
        val typeAliasNameIndex by lazy {
            proto.typeAliasList.withIndex()
                .groupBy({ context.nameResolver.getName(it.value.name) }) { (index) -> index }
        }
    }

    private val knownClassNamesInPackage = mutableMapOf<FqName, Set<String>?>()

    private fun hasTopLevelClassOf(classId: ClassId): Boolean {
        val knownNames = knownClassNamesInPackage.getOrPut(classId.packageFqName) {
            javaClassFinder.knownClassNamesInPackage(classId.packageFqName)
        } ?: return false
        return classId.relativeClassName.topLevelName() in knownNames
    }

    private fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData> {

        return packagePartProvider.findPackageParts(packageFqName.asString()).mapNotNull { partName ->
            val classId = ClassId.topLevel(JvmClassName.byInternalName(partName).fqNameForTopLevelClassMaybeWithDollars)
            if (!hasTopLevelClassOf(classId)) return@mapNotNull null
            val kotlinJvmBinaryClass = kotlinClassFinder.findKotlinClass(classId) ?: return@mapNotNull null

            val data = kotlinJvmBinaryClass.classHeader.data ?: return@mapNotNull null
            val strings = kotlinJvmBinaryClass.classHeader.strings ?: return@mapNotNull null
            val (nameResolver, packageProto) = JvmProtoBufUtil.readPackageDataFrom(data, strings)

            PackagePartsCacheData(
                packageProto,
                FirDeserializationContext.createForPackage(
                    packageFqName, packageProto, nameResolver, session,
                    JvmBinaryAnnotationDeserializer(session)
                )
            )
        }
    }

    override fun getClassUseSiteMemberScope(
        classId: ClassId,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope? {
        val symbol = this.getClassLikeSymbolByFqName(classId) ?: return null

        return symbol.firUnsafe<FirRegularClass>().buildDefaultUseSiteScope(session, scopeSession)
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): ConeClassLikeSymbol? {
        return findAndDeserializeClass(classId) ?: findAndDeserializeTypeAlias(classId)
    }

    private fun findAndDeserializeTypeAlias(
        classId: ClassId
    ): FirTypeAliasSymbol? {
        return typeAliasCache.getOrPut(classId) {
            getPackageParts(classId.packageFqName).firstNotNullResult { part ->
                val ids = part.typeAliasNameIndex[classId.shortClassName]
                if (ids == null || ids.isEmpty()) return@firstNotNullResult null
                val aliasProto = ids.map { part.proto.getTypeAlias(it) }.single()

                part.context.memberDeserializer.loadTypeAlias(aliasProto).symbol
            }
        }
    }

    private fun KotlinJvmBinaryClass.readClassDataFrom(): Pair<JvmNameResolver, ProtoBuf.Class>? {
        val data = classHeader.data ?: return null
        val strings = classHeader.strings ?: return null
        return JvmProtoBufUtil.readClassDataFrom(data, strings)
    }

    private fun ConeClassifierSymbol?.toDefaultResolvedTypeRef(classId: ClassId): FirResolvedTypeRef {
        return this?.let {
            FirResolvedTypeRefImpl(
                session, null, it.constructType(emptyList(), isNullable = false),
                annotations = emptyList()
            )
        } ?: FirErrorTypeRefImpl(session, null, "Symbol not found for $classId")

    }

    private fun loadAnnotation(
        annotationClassId: ClassId, result: MutableList<FirAnnotationCall>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        val lookupTag = ConeClassLikeLookupTagImpl(annotationClassId)
        val symbol = lookupTag.toSymbol(session)

        return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor {
            private val argumentMap = mutableMapOf<Name, FirExpression>()

            override fun visit(name: Name?, value: Any?) {
                if (name != null) {
                    argumentMap[name] = createConstant(value)
                }
            }

            private fun ClassLiteralValue.toFirClassReferenceExpression(): FirClassReferenceExpression {
                val literalLookupTag = ConeClassLikeLookupTagImpl(classId)
                val literalSymbol = literalLookupTag.toSymbol(this@KotlinDeserializedJvmSymbolsProvider.session)
                return FirClassReferenceExpressionImpl(
                    this@KotlinDeserializedJvmSymbolsProvider.session, null,
                    literalSymbol.toDefaultResolvedTypeRef(classId)
                )
            }

            private fun ClassId.toEnumEntryReferenceExpression(name: Name): FirExpression {
                return FirFunctionCallImpl(session, null).apply {
                    val entryClassId = createNestedClassId(name)
                    val entryLookupTag = ConeClassLikeLookupTagImpl(entryClassId)
                    val entryClassSymbol = entryLookupTag.toSymbol(this@KotlinDeserializedJvmSymbolsProvider.session)
                    val entryCallableSymbol =
                        this@KotlinDeserializedJvmSymbolsProvider.session.service<FirSymbolProvider>().getClassDeclaredCallableSymbols(
                            this@toEnumEntryReferenceExpression, name
                        ).firstOrNull()

                    this.calleeReference = when {
                        entryClassSymbol != null && (entryClassSymbol as? FirClassSymbol)?.fir is FirEnumEntry -> {
                            FirResolvedCallableReferenceImpl(
                                this@KotlinDeserializedJvmSymbolsProvider.session, null, name, entryClassSymbol
                            )
                        }
                        entryCallableSymbol != null -> {
                            FirResolvedCallableReferenceImpl(
                                this@KotlinDeserializedJvmSymbolsProvider.session, null, name, entryCallableSymbol
                            )
                        }
                        else -> {
                            FirErrorNamedReference(
                                this@KotlinDeserializedJvmSymbolsProvider.session, null,
                                errorReason = "Strange deserialized enum value: ${this@toEnumEntryReferenceExpression}.$name"
                            )
                        }
                    }
                }
            }

            override fun visitClassLiteral(name: Name, value: ClassLiteralValue) {
                argumentMap[name] = FirGetClassCallImpl(session, null).apply {
                    arguments += value.toFirClassReferenceExpression()
                }
            }

            override fun visitEnum(name: Name, enumClassId: ClassId, enumEntryName: Name) {
                argumentMap[name] = enumClassId.toEnumEntryReferenceExpression(enumEntryName)
            }

            override fun visitArray(name: Name): AnnotationArrayArgumentVisitor? {
                return object : AnnotationArrayArgumentVisitor {
                    private val elements = mutableListOf<FirExpression>()

                    override fun visit(value: Any?) {
                        elements.add(createConstant(value))
                    }

                    override fun visitEnum(enumClassId: ClassId, enumEntryName: Name) {
                        elements.add(enumClassId.toEnumEntryReferenceExpression(enumEntryName))
                    }

                    override fun visitClassLiteral(value: ClassLiteralValue) {
                        elements.add(value.toFirClassReferenceExpression())
                    }

                    override fun visitEnd() {
                        argumentMap[name] = FirArrayOfCallImpl(session, null).apply {
                            arguments += elements
                        }
                    }
                }
            }

            override fun visitAnnotation(name: Name, classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                val list = mutableListOf<FirAnnotationCall>()
                val visitor = loadAnnotation(classId, list)!!
                return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
                    override fun visitEnd() {
                        visitor.visitEnd()
                        argumentMap[name] = list.single()
                    }
                }
            }

            override fun visitEnd() {
                result += FirAnnotationCallImpl(session, null, null, symbol.toDefaultResolvedTypeRef(annotationClassId)).apply {
                    for ((name, expression) in argumentMap) {
                        arguments += FirNamedArgumentExpressionImpl(
                            this@KotlinDeserializedJvmSymbolsProvider.session, null, name, expression
                        )
                    }
                }
            }

            private fun createConstant(value: Any?): FirExpression {
                return value.createConstant(session)
            }
        }
    }

    private fun loadAnnotationIfNotSpecial(
        annotationClassId: ClassId, result: MutableList<FirAnnotationCall>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        if (annotationClassId in AbstractBinaryClassAnnotationAndConstantLoader.SPECIAL_ANNOTATIONS) return null
        return loadAnnotation(annotationClassId, result)
    }

    private fun findAndDeserializeClass(
        classId: ClassId,
        parentContext: FirDeserializationContext? = null
    ): FirClassSymbol? {
        if (!hasTopLevelClassOf(classId)) return null
        if (classesCache.containsKey(classId)) return classesCache[classId]


//        return classesCache.getOrPut(classId) {
        //return null
        val kotlinJvmBinaryClass = kotlinClassFinder.findKotlinClass(classId)
        if (kotlinJvmBinaryClass == null) {
            val outerClassId = classId.outerClassId ?: return null
            val outerJvmBinaryClass = kotlinClassFinder.findKotlinClass(outerClassId) ?: return null
            if (outerJvmBinaryClass.classHeader.kind != KotlinClassHeader.Kind.CLASS) return null
            val (nameResolver, outerClassProto) = outerJvmBinaryClass.readClassDataFrom() ?: return null
            if (outerClassProto.enumEntryList.none { nameResolver.getName(it.name) == classId.shortClassName }) {
                return null
            }

            val symbol = FirClassSymbol(classId)
            FirEnumEntryImpl(session, null, symbol, classId.shortClassName).apply {
                superTypeRefs += FirResolvedTypeRefImpl(
                    session,
                    null,
                    ConeClassTypeImpl(ConeClassLikeLookupTagImpl(outerClassId), emptyArray(), false),
                    emptyList()
                )
            }
            classesCache[classId] = symbol
        } else {
            if (kotlinJvmBinaryClass.classHeader.kind != KotlinClassHeader.Kind.CLASS) return null
            val (nameResolver, classProto) = kotlinJvmBinaryClass.readClassDataFrom() ?: return null

            val symbol = FirClassSymbol(classId)
            deserializeClassToSymbol(
                classId, classProto, symbol, nameResolver, session,
                JvmBinaryAnnotationDeserializer(session),
                parentContext, this::findAndDeserializeClass
            )
            classesCache[classId] = symbol
            val annotations = mutableListOf<FirAnnotationCall>()
            kotlinJvmBinaryClass.loadClassAnnotations(object : KotlinJvmBinaryClass.AnnotationVisitor {
                override fun visitAnnotation(classId: ClassId, source: SourceElement): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    return loadAnnotationIfNotSpecial(classId, annotations)
                }

                override fun visitEnd() {
                }


            }, null)
            (symbol.fir as FirAbstractAnnotatedElement).annotations += annotations
        }

        return classesCache[classId]
//        }
    }

    private fun loadFunctionsByName(part: PackagePartsCacheData, name: Name): List<FirCallableSymbol> {
        val functionIds = part.topLevelFunctionNameIndex[name] ?: return emptyList()
        return functionIds.map { part.proto.getFunction(it) }
            .map {
                part.context.memberDeserializer.loadFunction(it).symbol
            }
    }

    private fun loadPropertiesByName(part: PackagePartsCacheData, name: Name): List<FirCallableSymbol> {
        val propertyIds = part.topLevelPropertyNameIndex[name] ?: return emptyList()
        return propertyIds.map { part.proto.getProperty(it) }
            .map {
                part.context.memberDeserializer.loadProperty(it).symbol
            }
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<ConeCallableSymbol> {
        return getPackageParts(packageFqName).flatMap { part ->
            loadFunctionsByName(part, name) + loadPropertiesByName(part, name)
        }
    }

    override fun getClassDeclaredMemberScope(classId: ClassId) =
        findRegularClass(classId)?.let(::FirClassDeclaredMemberScope)

    private fun getPackageParts(packageFqName: FqName): Collection<PackagePartsCacheData> {
        return packagePartsCache.getOrPut(packageFqName) {
            computePackagePartsInfos(packageFqName)
        }
    }

    override fun getAllCallableNamesInPackage(fqName: FqName): Set<Name> {
        return getPackageParts(fqName).flatMapTo(mutableSetOf()) { packagePart ->
            packagePart.proto.functionList.map { packagePart.context.nameResolver.getName(it.name) }
        }
    }

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> =
        javaClassFinder.findPackage(fqName)
            ?.getClasses { true }.orEmpty()
            .mapTo(sortedSetOf(), JavaClass::name)

    private fun getClassDeclarations(classId: ClassId): List<FirDeclaration> {
        return findRegularClass(classId)?.declarations ?: emptyList()
    }


    private fun findRegularClass(classId: ClassId): FirRegularClass? =
        @Suppress("UNCHECKED_CAST")
        (getClassLikeSymbolByFqName(classId) as? FirBasedSymbol<FirRegularClass>)?.fir

    override fun getAllCallableNamesInClass(classId: ClassId): Set<Name> =
        getClassDeclarations(classId)
            .filterIsInstance<FirNamedDeclaration>()
            .mapTo(mutableSetOf(), FirNamedDeclaration::name)

    override fun getNestedClassesNamesInClass(classId: ClassId): Set<Name> {
        return getClassDeclarations(classId).filterIsInstance<FirRegularClass>().mapTo(mutableSetOf()) { it.name }
    }

    override fun getPackage(fqName: FqName): FqName? = null
}
