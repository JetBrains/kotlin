/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.builder.buildFromMissingDependenciesNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*
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

    private val constantCache = mutableMapOf<CallableId, FirExpression>()

    open fun loadConstant(property: KtProperty, callableId: CallableId): FirExpression? {
        if (!property.hasModifier(KtTokens.CONST_KEYWORD)) return null
        constantCache[callableId]?.let { return it }
        val propertyStub = property.stub as? KotlinPropertyStubImpl ?: return null
        val constantValue = propertyStub.constantInitializer ?: return null
        return resolveValue(constantValue)
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
        return deserializeAnnotation(
            userType.classId(),
            (ktAnnotation.stub as? KotlinAnnotationEntryStubImpl)?.valueArguments,
            useSiteTarget
        )
    }

    private fun deserializeAnnotation(
        classId: ClassId,
        valueArguments: Map<Name, ConstantValue<*>>?,
        useSiteTarget: AnnotationUseSiteTarget? = null
    ): FirAnnotation {
        return buildAnnotation {
            annotationTypeRef = buildResolvedTypeRef {
                type = classId.toLookupTag().constructClassType(emptyArray(), isNullable = false)
            }
            session.lazyDeclarationResolver.disableLazyResolveContractChecksInside {
                this.argumentMapping = buildAnnotationArgumentMapping {
                    valueArguments?.forEach { (name, constantValue) ->
                        mapping[name] = resolveValue(constantValue)
                    }
                }
            }
            useSiteTarget?.let {
                this.useSiteTarget = it
            }
        }
    }

    private fun resolveValue(
        value: ConstantValue<*>
    ): FirExpression {
        return when (value) {
            is EnumValue -> value.enumClassId.toEnumEntryReferenceExpression(value.enumEntryName)
            is KClassValue -> buildGetClassCall {
                val lookupTag = value.value.classId.toLookupTag()
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
            is ArrayValue -> buildArrayOfCall {
                argumentList = buildArgumentList {
                    value.value.mapTo(arguments) { resolveValue(it) }
                }
//                typeRef = buildResolvedTypeRef {
//                    type = expectedArrayElementType.createArrayType() //todo
//                }
            }
            //todo
            is AnnotationValue -> deserializeAnnotation(value.annoClassId, value.args.map { Name.identifier("value") to it }.toMap())
            is BooleanValue -> const(ConstantValueKind.Boolean, value.value, session.builtinTypes.booleanType)
            is ByteValue -> const(ConstantValueKind.Byte, value.value, session.builtinTypes.byteType)
            is CharValue -> const(ConstantValueKind.Char, value.value, session.builtinTypes.charType)
            is ShortValue -> const(ConstantValueKind.Short, value.value, session.builtinTypes.shortType)
            is LongValue -> const(ConstantValueKind.Long, value.value, session.builtinTypes.longType)
            is FloatValue -> const(ConstantValueKind.Float, value.value, session.builtinTypes.floatType)
            is DoubleValue -> const(ConstantValueKind.Double, value.value, session.builtinTypes.doubleType)
            is UByteValue -> const(ConstantValueKind.UnsignedByte, value.value, session.builtinTypes.byteType)
            is UShortValue -> const(ConstantValueKind.UnsignedShort, value.value, session.builtinTypes.shortType)
            is UIntValue -> const(ConstantValueKind.UnsignedInt, value.value, session.builtinTypes.intType)
            is ULongValue -> const(ConstantValueKind.UnsignedLong, value.value, session.builtinTypes.longType)
            is IntValue -> const(ConstantValueKind.Int, value.value, session.builtinTypes.intType)
            NullValue -> const(ConstantValueKind.Null, null, session.builtinTypes.nullableAnyType)
            is StringValue -> const(ConstantValueKind.String, value.value, session.builtinTypes.stringType)
            else -> error("Unexpected value $value")
        }
    }

    private fun <T> const(kind: ConstantValueKind<T>, value: T, typeRef: FirResolvedTypeRef): FirConstExpression<T> {
        return buildConstExpression(null, kind, value, setType = true).apply { this.replaceTypeRef(typeRef) }
    }

    private fun ClassId.toEnumEntryReferenceExpression(entryName: Name): FirExpression {
        return buildPropertyAccessExpression {
            val enumLookupTag = toLookupTag()
            val enumSymbol = enumLookupTag.toSymbol(session)
            val firClass = enumSymbol?.fir as? FirRegularClass
            val enumEntries = firClass?.collectEnumEntries() ?: emptyList()
            val enumEntrySymbol = enumEntries.find { it.name == entryName }
            calleeReference = enumEntrySymbol?.let {
                buildResolvedNamedReference {
                    name = entryName
                    resolvedSymbol = it.symbol
                }
            } ?: buildFromMissingDependenciesNamedReference {
                name = entryName
            }
            if (enumEntrySymbol != null) {
                typeRef = enumEntrySymbol.returnTypeRef
            }
        }
    }
}
