/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.impl.FirSimpleFunctionImpl
import org.jetbrains.kotlin.fir.deserialization.FirDeserializationContext
import org.jetbrains.kotlin.fir.deserialization.deserializeClassToSymbol
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirClassReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.buildUnaryArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.createConstant
import org.jetbrains.kotlin.fir.java.topLevelName
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.AbstractFirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredCallableSymbols
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassType
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmNameResolver
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.isOneSegmentFQN
import org.jetbrains.kotlin.resolve.constants.ClassLiteralValue
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.kotlin.utils.getOrPutNullable

class KotlinDeserializedJvmSymbolsProvider(
    val session: FirSession,
    val project: Project,
    private val packagePartProvider: PackagePartProvider,
    private val javaSymbolProvider: JavaSymbolProvider,
    private val kotlinClassFinder: KotlinClassFinder,
    private val javaClassFinder: JavaClassFinder,
    private val kotlinScopeProvider: KotlinScopeProvider,
) : AbstractFirSymbolProvider<FirClassLikeSymbol<*>>() {
    private val classesCache = HashMap<ClassId, FirRegularClassSymbol>()
    private val typeAliasCache = HashMap<ClassId, FirTypeAliasSymbol?>()
    private val packagePartsCache = HashMap<FqName, Collection<PackagePartsCacheData>>()

    private val handledByJava = HashSet<ClassId>()

    private class PackagePartsCacheData(
        val proto: ProtoBuf.Package,
        val context: FirDeserializationContext,
        val source: JvmPackagePartSource,
    ) {
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

    // This function returns true if we are sure that no top-level class with this id is available
    // If it returns false, it means we can say nothing about this id
    private fun hasNoTopLevelClassOf(classId: ClassId): Boolean {
        val knownNames = knownClassNamesInPackage.getOrPut(classId.packageFqName) {
            javaClassFinder.knownClassNamesInPackage(classId.packageFqName)
        } ?: return false
        return classId.relativeClassName.topLevelName() !in knownNames
    }

    private fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData> {

        return packagePartProvider.findPackageParts(packageFqName.asString()).mapNotNull { partName ->
            val classId = ClassId.topLevel(JvmClassName.byInternalName(partName).fqNameForTopLevelClassMaybeWithDollars)
            if (hasNoTopLevelClassOf(classId)) return@mapNotNull null
            val (kotlinJvmBinaryClass, byteContent) =
                kotlinClassFinder.findKotlinClassOrContent(classId) as? KotlinClassFinder.Result.KotlinClass ?: return@mapNotNull null

            val header = kotlinJvmBinaryClass.classHeader
            val data = header.data ?: header.incompatibleData ?: return@mapNotNull null
            val strings = header.strings ?: return@mapNotNull null
            val (nameResolver, packageProto) = JvmProtoBufUtil.readPackageDataFrom(data, strings)

            val source = JvmPackagePartSource(
                kotlinJvmBinaryClass, packageProto, nameResolver,
                kotlinJvmBinaryClass.incompatibility, kotlinJvmBinaryClass.isPreReleaseInvisible,
            )

            PackagePartsCacheData(
                packageProto,
                FirDeserializationContext.createForPackage(
                    packageFqName, packageProto, nameResolver, session,
                    JvmBinaryAnnotationDeserializer(session, kotlinJvmBinaryClass, byteContent),
                    source
                ),
                source,
            )
        }
    }

    private val KotlinJvmBinaryClass.incompatibility: IncompatibleVersionErrorData<JvmMetadataVersion>?
        get() {
            // TODO: skipMetadataVersionCheck
            if (classHeader.metadataVersion.isCompatible()) return null
            return IncompatibleVersionErrorData(classHeader.metadataVersion, JvmMetadataVersion.INSTANCE, location, classId)
        }

    private val KotlinJvmBinaryClass.isPreReleaseInvisible: Boolean
        get() = classHeader.isPreRelease

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>? {
        return findAndDeserializeClass(classId) ?: findAndDeserializeTypeAlias(classId)
    }

    private fun findAndDeserializeTypeAlias(
        classId: ClassId,
    ): FirTypeAliasSymbol? {
        if (!classId.relativeClassName.isOneSegmentFQN()) return null
        return typeAliasCache.getOrPutNullable(classId) {
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

    private fun ConeClassLikeLookupTag.toDefaultResolvedTypeRef(classId: ClassId): FirResolvedTypeRef =
        buildResolvedTypeRef {
            type = constructClassType(emptyArray(), isNullable = false)
        }

    private fun loadAnnotation(
        annotationClassId: ClassId, result: MutableList<FirAnnotationCall>,
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        val lookupTag = ConeClassLikeLookupTagImpl(annotationClassId)

        return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor {
            private val argumentMap = mutableMapOf<Name, FirExpression>()

            override fun visit(name: Name?, value: Any?) {
                if (name != null) {
                    argumentMap[name] = createConstant(value)
                }
            }

            private fun ClassLiteralValue.toFirClassReferenceExpression(): FirClassReferenceExpression {
                val literalLookupTag = ConeClassLikeLookupTagImpl(classId)
                return buildClassReferenceExpression {
                    classTypeRef = literalLookupTag.toDefaultResolvedTypeRef(classId)
                }
            }

            private fun ClassId.toEnumEntryReferenceExpression(name: Name): FirExpression {
                return buildFunctionCall {
                    val entryCallableSymbol =
                        this@KotlinDeserializedJvmSymbolsProvider.session.firSymbolProvider.getClassDeclaredCallableSymbols(
                            this@toEnumEntryReferenceExpression, name,
                        ).firstOrNull()

                    calleeReference = when {
                        entryCallableSymbol != null -> {
                            buildResolvedNamedReference {
                                this.name = name
                                resolvedSymbol = entryCallableSymbol
                            }
                        }
                        else -> {
                            buildErrorNamedReference {
                                diagnostic = ConeSimpleDiagnostic(
                                    "Strange deserialized enum value: ${this@toEnumEntryReferenceExpression}.$name",
                                    DiagnosticKind.Java,
                                )
                            }
                        }
                    }
                }
            }

            override fun visitClassLiteral(name: Name, value: ClassLiteralValue) {
                argumentMap[name] = buildGetClassCall {
                    argumentList = buildUnaryArgumentList(value.toFirClassReferenceExpression())
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
                        elements.add(
                            buildGetClassCall {
                                argumentList = buildUnaryArgumentList(value.toFirClassReferenceExpression())
                            }
                        )
                    }

                    override fun visitEnd() {
                        argumentMap[name] = buildArrayOfCall {
                            argumentList = buildArgumentList {
                                arguments += elements
                            }
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
                result += buildAnnotationCall {
                    annotationTypeRef = lookupTag.toDefaultResolvedTypeRef(annotationClassId)
                    argumentList = buildArgumentList {
                        for ((name, expression) in argumentMap) {
                            arguments += buildNamedArgumentExpression {
                                this.expression = expression
                                this.name = name
                                isSpread = false
                            }
                        }
                    }
                }
            }

            private fun createConstant(value: Any?): FirExpression {
                return value.createConstant(session)
            }
        }
    }

    internal fun loadAnnotationIfNotSpecial(
        annotationClassId: ClassId, result: MutableList<FirAnnotationCall>,
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        if (annotationClassId in AbstractBinaryClassAnnotationAndConstantLoader.SPECIAL_ANNOTATIONS) return null
        return loadAnnotation(annotationClassId, result)
    }

    private fun findAndDeserializeClass(
        classId: ClassId,
        parentContext: FirDeserializationContext? = null,
    ): FirRegularClassSymbol? {
        if (hasNoTopLevelClassOf(classId)) return null
        if (classesCache.containsKey(classId)) return classesCache[classId]

        if (classId in handledByJava) return null

        val result = try {
            kotlinClassFinder.findKotlinClassOrContent(classId)
        } catch (e: ProcessCanceledException) {
            return null
        }
        val kotlinClassWithContent = when (result) {
            is KotlinClassFinder.Result.KotlinClass -> result
            is KotlinClassFinder.Result.ClassFileContent -> {
                handledByJava.add(classId)
                return try {
                    javaSymbolProvider.getFirJavaClass(classId, result)
                } catch (e: ProcessCanceledException) {
                    null
                }
            }
            null -> null
        }
        if (kotlinClassWithContent == null) {
            val outerClassId = classId.outerClassId ?: return null
            findAndDeserializeClass(outerClassId) ?: return null
        } else {
            val (kotlinJvmBinaryClass, byteContent) = kotlinClassWithContent

            if (kotlinJvmBinaryClass.classHeader.kind != KotlinClassHeader.Kind.CLASS) return null
            val (nameResolver, classProto) = kotlinJvmBinaryClass.readClassDataFrom() ?: return null

            val symbol = FirRegularClassSymbol(classId)
            deserializeClassToSymbol(
                classId, classProto, symbol, nameResolver, session,
                JvmBinaryAnnotationDeserializer(session, kotlinJvmBinaryClass, byteContent),
                kotlinScopeProvider,
                parentContext, KotlinJvmBinarySourceElement(kotlinJvmBinaryClass),
                this::findAndDeserializeClass
            )

            classesCache[classId] = symbol
            val annotations = mutableListOf<FirAnnotationCall>()
            kotlinJvmBinaryClass.loadClassAnnotations(
                object : KotlinJvmBinaryClass.AnnotationVisitor {
                    override fun visitAnnotation(classId: ClassId, source: SourceElement): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                        return loadAnnotationIfNotSpecial(classId, annotations)
                    }

                    override fun visitEnd() {
                    }


                },
                byteContent,
            )
            (symbol.fir.annotations as MutableList<FirAnnotationCall>) += annotations
        }

        return classesCache[classId]
    }

    private fun loadFunctionsByName(part: PackagePartsCacheData, name: Name): List<FirCallableSymbol<*>> {
        val functionIds = part.topLevelFunctionNameIndex[name] ?: return emptyList()
        return functionIds.map { part.proto.getFunction(it) }
            .map {
                val firNamedFunction = part.context.memberDeserializer.loadFunction(it) as FirSimpleFunctionImpl
                firNamedFunction.symbol
            }
    }

    private fun loadPropertiesByName(part: PackagePartsCacheData, name: Name): List<FirCallableSymbol<*>> {
        val propertyIds = part.topLevelPropertyNameIndex[name] ?: return emptyList()
        return propertyIds.map { part.proto.getProperty(it) }
            .map {
                val firProperty = part.context.memberDeserializer.loadProperty(it)
                firProperty.symbol
            }
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        return getPackageParts(packageFqName).flatMap { part ->
            loadFunctionsByName(part, name) + loadPropertiesByName(part, name)
        }
    }

    override fun getNestedClassifierScope(classId: ClassId): FirScope? {
        return findRegularClass(classId)?.let {
            nestedClassifierScope(it)
        }
    }

    private fun getPackageParts(packageFqName: FqName): Collection<PackagePartsCacheData> {
        return packagePartsCache.getOrPut(packageFqName) {
            try {
                computePackagePartsInfos(packageFqName)
            } catch (e: ProcessCanceledException) {
                emptyList()
            }
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
        getClassLikeSymbolByFqName(classId)?.fir as? FirRegularClass

    override fun getAllCallableNamesInClass(classId: ClassId): Set<Name> =
        getClassDeclarations(classId).mapNotNullTo(mutableSetOf()) {
            when (it) {
                is FirSimpleFunction -> it.name
                is FirVariable<*> -> it.name
                else -> null
            }
        }

    override fun getNestedClassesNamesInClass(classId: ClassId): Set<Name> {
        return getClassDeclarations(classId).filterIsInstance<FirRegularClass>().mapTo(mutableSetOf()) { it.name }
    }

    override fun getPackage(fqName: FqName): FqName? = null
}
