/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredPropertySymbols
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.KotlinNameReferenceExpressionStub
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderWithTextStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.types.ConstantValueKind

abstract class StubBasedAbstractAnnotationDeserializer(
    private val session: FirSession,
) {
    open fun inheritAnnotationInfo(parent: StubBasedAbstractAnnotationDeserializer) {
    }

    enum class CallableKind {
        PROPERTY,
        PROPERTY_GETTER,
        PROPERTY_SETTER,
        OTHERS
    }

    fun loadClassAnnotations(classOrObject: KtClassOrObject): List<FirAnnotation> {
        val ktAnnotations = classOrObject.annotationEntries
        if (ktAnnotations.isEmpty()) return emptyList()
        return ktAnnotations.map { deserializeAnnotation(it) }
    }

    fun loadTypeAliasAnnotations(typeAlias: KtTypeAlias): List<FirAnnotation> {
        val annotations = typeAlias.annotationEntries
        if (annotations.isEmpty()) return emptyList()
        return annotations.map { deserializeAnnotation(it) }
    }

    open fun loadFunctionAnnotations(
        ktFunction: KtFunction
    ): List<FirAnnotation> {

        val annotations = ktFunction.annotationEntries
        if (annotations.isEmpty()) return emptyList()
        return annotations.map { deserializeAnnotation(it) }
    }

    open fun loadPropertyAnnotations(
        ktProperty: KtProperty,
        containingClassProto: KtClassOrObject?
    ): List<FirAnnotation> {
        val annotations = ktProperty.annotationEntries
        if (annotations.isEmpty()) return emptyList()
        return annotations.map { deserializeAnnotation(it, AnnotationUseSiteTarget.PROPERTY) }
    }

    open fun loadPropertyBackingFieldAnnotations(
        propertyProto: KtProperty
    ): List<FirAnnotation> {
        return emptyList()
    }

    open fun loadPropertyDelegatedFieldAnnotations(
        property: KtProperty
    ): List<FirAnnotation> {
        return emptyList()
    }

    open fun loadPropertyGetterAnnotations(
        ktProperty: KtPropertyAccessor,
    ): List<FirAnnotation> {
        val annotations = ktProperty.annotationEntries
        if (annotations.isEmpty()) return emptyList()
        return annotations.map { deserializeAnnotation(it, AnnotationUseSiteTarget.PROPERTY_GETTER) }
    }

    open fun loadPropertySetterAnnotations(
        ktProperty: KtPropertyAccessor
    ): List<FirAnnotation> {
        val annotations = ktProperty.annotationEntries
        if (annotations.isEmpty()) return emptyList()
        return annotations.map { deserializeAnnotation(it, AnnotationUseSiteTarget.PROPERTY_SETTER) }
    }

    open fun loadConstructorAnnotations(
        constructor: KtConstructor<*>
    ): List<FirAnnotation> {
        val annotations = constructor.annotationEntries
        if (annotations.isEmpty()) return emptyList()
        return annotations.map { deserializeAnnotation(it) }
    }

    open fun loadValueParameterAnnotations(
        valueParameterProto: KtParameter,
        classProto: KtClassOrObject?,
        kind: CallableKind,
        parameterIndex: Int
    ): List<FirAnnotation> {
        val annotations = valueParameterProto.annotationEntries
        if (annotations.isEmpty()) return emptyList()
        return annotations.map { deserializeAnnotation(it) }
    }

    open fun loadExtensionReceiverParameterAnnotations(
        callableProto: KtCallableDeclaration,
        kind: CallableKind
    ): List<FirAnnotation> {
        return emptyList()
    }

    abstract fun loadTypeAnnotations(typeReference: KtTypeReference): List<FirAnnotation>

    open fun loadTypeParameterAnnotations(typeParameterProto: KtTypeParameter) =
        emptyList<FirAnnotation>()

    fun deserializeAnnotation(
        ktAnnotation: KtAnnotationEntry,
        useSiteTarget: AnnotationUseSiteTarget? = null
    ): FirAnnotation {
        val userType =
            ktAnnotation.getStubOrPsiChild(KtStubElementTypes.CONSTRUCTOR_CALLEE)?.getStubOrPsiChild(KtStubElementTypes.TYPE_REFERENCE)
                ?.getStubOrPsiChild(KtStubElementTypes.USER_TYPE)!!
        val classId = userType.stub!!.classId()!!//todo
        return buildAnnotation {
            annotationTypeRef = buildResolvedTypeRef {
                type = classId.toLookupTag().constructClassType(emptyArray(), isNullable = false)
            }
            session.lazyDeclarationResolver.disableLazyResolveContractChecksInside {
                this.argumentMapping = createArgumentMapping(ktAnnotation, classId)
            }
            useSiteTarget?.let {
                this.useSiteTarget = it
            }
        }
    }

    private fun createArgumentMapping(
        ktAnnotation: KtAnnotationEntry,
        classId: ClassId
    ): FirAnnotationArgumentMapping {
        return buildAnnotationArgumentMapping build@{
            if (ktAnnotation.valueArguments.isEmpty()) return@build
            // Used only for annotation parameters of array types
            // Avoid triggering it in other cases, since it's quite expensive
            val parameterByName: Map<Name, FirValueParameter>? by lazy(LazyThreadSafetyMode.NONE) {
                val lookupTag = classId.toLookupTag()
                val symbol = lookupTag.toSymbol(session)
                val firAnnotationClass = (symbol as? FirRegularClassSymbol)?.fir ?: return@lazy null

                val classScope =
                    firAnnotationClass.defaultType()
                        .scope(session, ScopeSession(), FakeOverrideTypeCalculator.DoNothing, requiredPhase = null)
                        ?: error("Null scope for $classId")

                val constructor =
                    classScope.getDeclaredConstructors().singleOrNull()?.fir ?: error("No single constructor found for $classId")

                constructor.valueParameters.associateBy { it.name }
            }

            ktAnnotation.valueArguments.groupBy { it.getArgumentName()?.asName ?: Name.identifier("value") }
                .mapNotNull { (name, valueArguments) ->
                    val expectedType = { parameterByName?.get(name)?.returnTypeRef?.coneType }
                    val values = valueArguments.map {
                        resolveValue(it.getArgumentExpression(), expectedType)
                    }
                    val value = if (values.size == 1) values[0] else buildArrayOfCall {
                        argumentList = buildArgumentList {
                            arguments.addAll(values)
                        }
                        typeRef = buildResolvedTypeRef {
                            type = (/*expectedType() ?: */session.builtinTypes.anyType.type).createArrayType()
                        }
                    }
                    name to value
                }.toMap(mapping)
        }
    }

    private fun resolveValue(
        value: KtExpression?, expectedType: () -> ConeKotlinType?
    ): FirExpression {
        if (value == null) error("Unexpected")
        if (value is KtQualifiedExpression) {
            val receiverExpression = value.receiverExpression
            if (receiverExpression is KtQualifiedExpression) {
                val selectorExpression = value.selectorExpression
                val classId = traverseClassId(receiverExpression)
                val name = (selectorExpression as KtNameReferenceExpression).getReferencedNameAsName()
                if (!Name.isValidIdentifier(name.asString())) {
                    return buildGetClassCall {
                        val lookupTag = classId.toLookupTag()
                        val referencedType = lookupTag.constructType(emptyArray(), isNullable = false)
                        val resolvedTypeRef = buildResolvedTypeRef {
                            type = StandardClassIds.KClass.constructClassLikeType(arrayOf(referencedType), false)
                        }
                        argumentList = buildUnaryArgumentList(
                            buildClassReferenceExpression {
                                classTypeRef = buildResolvedTypeRef { type = referencedType }
                                typeRef = resolvedTypeRef
                            }
                        )
                    }
                }
                return classId.toEnumEntryReferenceExpression(name)
            }
        }
        if (value is KtStringTemplateExpression) {
            val textStub = value.entries[0].stub as KotlinPlaceHolderWithTextStub<*>
            return const(ConstantValueKind.String, textStub.text(), session.builtinTypes.stringType)
        }
//        val isUnsigned = Flags.IS_UNSIGNED.get(value.flags)
//
//        return when (value.type) {
//            BYTE -> {
//                val kind = if (isUnsigned) ConstantValueKind.UnsignedByte else ConstantValueKind.Byte
//                const(kind, value.intValue.toByte(), session.builtinTypes.byteType)
//            }
//
//            SHORT -> {
//                val kind = if (isUnsigned) ConstantValueKind.UnsignedShort else ConstantValueKind.Short
//                const(kind, value.intValue.toShort(), session.builtinTypes.shortType)
//            }
//
//            INT -> {
//                val kind = if (isUnsigned) ConstantValueKind.UnsignedInt else ConstantValueKind.Int
//                const(kind, value.intValue.toInt(), session.builtinTypes.intType)
//            }
//
//            LONG -> {
//                val kind = if (isUnsigned) ConstantValueKind.UnsignedLong else ConstantValueKind.Long
//                const(kind, value.intValue, session.builtinTypes.longType)
//            }
//
//            CHAR -> const(ConstantValueKind.Char, value.intValue.toInt().toChar(), session.builtinTypes.charType)
//            FLOAT -> const(ConstantValueKind.Float, value.floatValue, session.builtinTypes.floatType)
//            DOUBLE -> const(ConstantValueKind.Double, value.doubleValue, session.builtinTypes.doubleType)
//            BOOLEAN -> const(ConstantValueKind.Boolean, (value.intValue != 0L), session.builtinTypes.booleanType)
//            STRING -> const(ConstantValueKind.String, nameResolver.getString(value.stringValue), session.builtinTypes.stringType)
//            ANNOTATION -> deserializeAnnotation(value.annotation, nameResolver)
//            CLASS -> buildGetClassCall {
//                val classId = nameResolver.getClassId(value.classId)
//                val lookupTag = classId.toLookupTag()
//                val referencedType = lookupTag.constructType(emptyArray(), isNullable = false)
//                val resolvedTypeRef = buildResolvedTypeRef {
//                    type = StandardClassIds.KClass.constructClassLikeType(arrayOf(referencedType), false)
//                }
//                argumentList = buildUnaryArgumentList(
//                    buildClassReferenceExpression {
//                        classTypeRef = buildResolvedTypeRef { type = referencedType }
//                        typeRef = resolvedTypeRef
//                    }
//                )
//                typeRef = resolvedTypeRef
//            }
//            ENUM -> buildFunctionCall {
//                val classId = nameResolver.getClassId(value.classId)
//                val entryName = nameResolver.getName(value.enumValueId)
//
//                val enumLookupTag = classId.toLookupTag()
//                val enumSymbol = enumLookupTag.toSymbol(session)
//                val firClass = enumSymbol?.fir as? FirRegularClass
//                val enumEntries = firClass?.collectEnumEntries() ?: emptyList()
//                val enumEntrySymbol = enumEntries.find { it.name == entryName }
//                calleeReference = enumEntrySymbol?.let {
//                    buildResolvedNamedReference {
//                        name = entryName
//                        resolvedSymbol = it.symbol
//                    }
//                } ?: buildErrorNamedReference {
//                    diagnostic =
//                        ConeSimpleDiagnostic("Strange deserialized enum value: $classId.$entryName", DiagnosticKind.DeserializationError)
//                }
//                if (enumEntrySymbol != null) {
//                    typeRef = enumEntrySymbol.returnTypeRef
//                }
//            }
//            ARRAY -> {
//                val expectedArrayElementType = expectedType()?.arrayElementType() ?: session.builtinTypes.anyType.type
//                buildArrayOfCall {
//                    argumentList = buildArgumentList {
//                        value.arrayElementList.mapTo(arguments) { resolveValue(it) { expectedArrayElementType } }
//                    }
//                    typeRef = buildResolvedTypeRef {
//                        type = expectedArrayElementType.createArrayType()
//                    }
//                }
//            }
//
//            else -> error("Unsupported annotation argument type: ${value.type} (expected $expectedType)")
//        }
        error("Not yet implemented $value $expectedType")
    }

    private fun traverseClassId(receiverExpression: KtExpression): ClassId {
        var receiver = receiverExpression
        val packageSegments = mutableListOf<String>()
        val classSegments = mutableListOf<String>()
        while (receiver is KtQualifiedExpression) {
            val rExpr = receiver.receiverExpression
            if (rExpr is KtNameReferenceExpression) {
                val referencedName = rExpr.getReferencedName()
                val stub = rExpr.stub
                if (stub is KotlinNameReferenceExpressionStub && stub.isClassRef()) {
                    classSegments.add(referencedName)
                } else {
                    packageSegments.add(referencedName)
                }
                break
            }
            val selectorExpression = receiver.selectorExpression
            val referencedName = (selectorExpression as KtNameReferenceExpression).getReferencedName()
            val stub = selectorExpression.stub
            if (stub is KotlinNameReferenceExpressionStub && stub.isClassRef()) {
                classSegments.add(referencedName)
            } else {
                packageSegments.add(referencedName)
            }
            receiver = receiver.receiverExpression
        }
        val packageFQN = FqName.fromSegments(packageSegments)
        val relativeName = FqName.fromSegments(classSegments)
        return ClassId(packageFQN, relativeName, false)
    }

    private fun <T> const(kind: ConstantValueKind<T>, value: T, typeRef: FirResolvedTypeRef): FirConstExpression<T> {
        return buildConstExpression(null, kind, value, setType = true).apply { this.replaceTypeRef(typeRef) }
    }

    private fun ClassId.toEnumEntryReferenceExpression(name: Name): FirExpression {
        return buildFunctionCall {
            val entryPropertySymbol =
                session.symbolProvider.getClassDeclaredPropertySymbols(
                    this@toEnumEntryReferenceExpression, name,
                ).firstOrNull()

            calleeReference = when {
                entryPropertySymbol != null -> {
                    buildResolvedNamedReference {
                        this.name = name
                        resolvedSymbol = entryPropertySymbol
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

            typeRef = buildResolvedTypeRef {
                type = ConeClassLikeTypeImpl(
                    this@toEnumEntryReferenceExpression.toLookupTag(),
                    emptyArray(),
                    isNullable = false
                )
            }
        }
    }
}
