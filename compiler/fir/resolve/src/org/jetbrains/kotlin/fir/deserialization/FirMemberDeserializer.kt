/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.getName

class FirDeserializationContext(
    val nameResolver: NameResolver,
    val typeTable: TypeTable,
    val versionRequirementTable: VersionRequirementTable,
    val session: FirSession,
    val packageFqName: FqName,
    val relativeClassName: FqName?,
    val typeDeserializer: FirTypeDeserializer,
    val annotationDeserializer: AbstractAnnotationDeserializer,
    val containerSource: DeserializedContainerSource?,
    val components: FirDeserializationComponents
) {
    fun childContext(
        typeParameterProtos: List<ProtoBuf.TypeParameter>,
        nameResolver: NameResolver = this.nameResolver,
        typeTable: TypeTable = this.typeTable,
        relativeClassName: FqName? = this.relativeClassName
    ): FirDeserializationContext = FirDeserializationContext(
        nameResolver, typeTable, versionRequirementTable, session, packageFqName, relativeClassName,
        FirTypeDeserializer(
            session, nameResolver, typeTable, typeParameterProtos, typeDeserializer
        ),
        annotationDeserializer, containerSource, components
    )

    val memberDeserializer: FirMemberDeserializer = FirMemberDeserializer(this)

    companion object {
        fun createForPackage(
            fqName: FqName,
            packageProto: ProtoBuf.Package,
            nameResolver: NameResolver,
            session: FirSession,
            annotationDeserializer: AbstractAnnotationDeserializer,
            containerSource: DeserializedContainerSource?
        ) = createRootContext(
            nameResolver,
            TypeTable(packageProto.typeTable),
            session,
            annotationDeserializer,
            fqName,
            relativeClassName = null,
            typeParameterProtos = emptyList(),
            containerSource
        )

        fun createForClass(
            classId: ClassId,
            classProto: ProtoBuf.Class,
            nameResolver: NameResolver,
            session: FirSession,
            annotationDeserializer: AbstractAnnotationDeserializer,
            containerSource: DeserializedContainerSource?
        ) = createRootContext(
            nameResolver,
            TypeTable(classProto.typeTable),
            session,
            annotationDeserializer,
            classId.packageFqName,
            classId.relativeClassName,
            classProto.typeParameterList,
            containerSource
        )

        private fun createRootContext(
            nameResolver: NameResolver,
            typeTable: TypeTable,
            session: FirSession,
            annotationDeserializer: AbstractAnnotationDeserializer,
            packageFqName: FqName,
            relativeClassName: FqName?,
            typeParameterProtos: List<ProtoBuf.TypeParameter>,
            containerSource: DeserializedContainerSource?
        ): FirDeserializationContext {
            return FirDeserializationContext(
                nameResolver, typeTable,
                VersionRequirementTable.EMPTY, // TODO:
                session,
                packageFqName,
                relativeClassName,
                FirTypeDeserializer(
                    session,
                    nameResolver,
                    typeTable,
                    typeParameterProtos,
                    null
                ),
                annotationDeserializer,
                containerSource,
                FirDeserializationComponents()
            )
        }
    }
}

// TODO: Move something here
class FirDeserializationComponents {

}

class FirMemberDeserializer(private val c: FirDeserializationContext) {
    private val contractDeserializer = FirContractDeserializer(c)

    private fun loadOldFlags(oldFlags: Int): Int {
        val lowSixBits = oldFlags and 0x3f
        val rest = (oldFlags shr 8) shl 6
        return lowSixBits + rest
    }

    fun loadTypeAlias(proto: ProtoBuf.TypeAlias): FirTypeAlias {
        val flags = proto.flags
        val name = c.nameResolver.getName(proto.name)
        val local = c.childContext(proto.typeParameterList)
        return buildTypeAlias {
            session = c.session
            this.name = name
            status = FirDeclarationStatusImpl(ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags)), Modality.FINAL).apply {
                isExpect = Flags.IS_EXPECT_CLASS.get(flags)
                isActual = false
            }
            symbol = FirTypeAliasSymbol(ClassId(c.packageFqName, name))
            expandedTypeRef = buildResolvedTypeRef {
                type = local.typeDeserializer.type(proto.underlyingType(c.typeTable))
            }
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.fir }
        }
    }

    fun loadProperty(proto: ProtoBuf.Property): FirProperty {
        val flags = if (proto.hasFlags()) proto.flags else loadOldFlags(proto.oldFlags)
        val callableName = c.nameResolver.getName(proto.name)
        val symbol = FirPropertySymbol(CallableId(c.packageFqName, c.relativeClassName, callableName))
        val local = c.childContext(proto.typeParameterList)

        val getterFlags = if (proto.hasGetterFlags()) proto.getterFlags else flags
        val setterFlags = if (proto.hasSetterFlags()) proto.setterFlags else flags
        val isVar = Flags.IS_VAR.get(flags)
        return buildProperty {
            session = c.session
            returnTypeRef = proto.returnType(c.typeTable).toTypeRef(local)
            receiverTypeRef = proto.receiverType(c.typeTable)?.toTypeRef(local)
            name = callableName
            this.isVar = isVar
            this.symbol = symbol
            isLocal = false
            status = FirDeclarationStatusImpl(
                ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags)),
                ProtoEnumFlags.modality(Flags.MODALITY.get(flags))
            ).apply {
                isExpect = Flags.IS_EXPECT_PROPERTY.get(flags)
                isActual = false
                isOverride = false
                isConst = Flags.IS_CONST.get(flags)
                isLateInit = Flags.IS_LATEINIT.get(flags)
            }

            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.fir }
            annotations += c.annotationDeserializer.loadPropertyAnnotations(proto, local.nameResolver)
            getter = FirDefaultPropertyGetter(null, c.session, returnTypeRef, ProtoEnumFlags.visibility(Flags.VISIBILITY.get(getterFlags)))
            setter = if (isVar) {
                FirDefaultPropertySetter(null, c.session, returnTypeRef, ProtoEnumFlags.visibility(Flags.VISIBILITY.get(setterFlags)))
            } else null
            this.containerSource = c.containerSource
        }
    }

    fun loadFunction(proto: ProtoBuf.Function): FirSimpleFunction {
        val flags = if (proto.hasFlags()) proto.flags else loadOldFlags(proto.oldFlags)

        val receiverAnnotations =
            // TODO: support annotations
            Annotations.EMPTY

        val versionRequirementTable =
            // TODO: Support case for KOTLIN_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME
            c.versionRequirementTable

        val callableName = c.nameResolver.getName(proto.name)
        val symbol = FirNamedFunctionSymbol(CallableId(c.packageFqName, c.relativeClassName, callableName))
        val local = c.childContext(proto.typeParameterList)
        // TODO: support contracts

        val simpleFunction = buildSimpleFunction {
            session = c.session
            returnTypeRef = proto.returnType(local.typeTable).toTypeRef(local)
            receiverTypeRef = proto.receiverType(local.typeTable)?.toTypeRef(local)
            name = callableName
            status = FirDeclarationStatusImpl(
                ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags)),
                ProtoEnumFlags.modality(Flags.MODALITY.get(flags))
            ).apply {
                isExpect = Flags.IS_EXPECT_FUNCTION.get(flags)
                isActual = false
                isOverride = false
                isOperator = Flags.IS_OPERATOR.get(flags)
                isInfix = Flags.IS_INFIX.get(flags)
                isInline = Flags.IS_INLINE.get(flags)
                isTailRec = Flags.IS_TAILREC.get(flags)
                isExternal = Flags.IS_EXTERNAL_FUNCTION.get(flags)
                isSuspend = Flags.IS_SUSPEND.get(flags)
            }
            this.symbol = symbol
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.fir }
            valueParameters += local.memberDeserializer.valueParameters(proto.valueParameterList)
            annotations += local.annotationDeserializer.loadFunctionAnnotations(proto, local.nameResolver)
            this.containerSource = c.containerSource
        }
        if (proto.hasContract()) {
            val contractDescription = contractDeserializer.loadContract(proto.contract, simpleFunction)
            if (contractDescription != null) {
                simpleFunction.replaceContractDescription(contractDescription)
            }
        }
        return simpleFunction
    }

    fun loadConstructor(proto: ProtoBuf.Constructor, classBuilder: AbstractFirRegularClassBuilder): FirConstructor {
        val flags = proto.flags
        val relativeClassName = c.relativeClassName!!
        val symbol = FirConstructorSymbol(CallableId(c.packageFqName, relativeClassName, relativeClassName.shortName()))
        val local = c.childContext(emptyList())
        val isPrimary = !Flags.IS_SECONDARY.get(flags)

        val typeParameters = classBuilder.typeParameters

        val delegatedSelfType = buildResolvedTypeRef {
            type = ConeClassLikeTypeImpl(
                classBuilder.symbol.toLookupTag(),
                typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }.toTypedArray(),
                false
            )
        }

        return if (isPrimary) {
            FirPrimaryConstructorBuilder()
        } else {
            FirConstructorBuilder()
        }.apply {
            session = c.session
            returnTypeRef = delegatedSelfType
            status = FirDeclarationStatusImpl(ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags)), Modality.FINAL).apply {
                isExpect = Flags.IS_EXPECT_FUNCTION.get(flags)
                isActual = false
                isInner = classBuilder.status.isInner
            }
            this.symbol = symbol
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            this.typeParameters += typeParameters
            valueParameters += local.memberDeserializer.valueParameters(
                proto.valueParameterList, addDefaultValue = classBuilder.symbol.classId == StandardClassIds.Enum
            )
            annotations += local.annotationDeserializer.loadConstructorAnnotations(proto, local.nameResolver)
        }.build()
    }

    private fun defaultValue(flags: Int): FirExpression? {
        if (Flags.DECLARES_DEFAULT_VALUE.get(flags)) {
            return buildExpressionStub()
        }
        return null
    }

    private fun valueParameters(
        valueParameters: List<ProtoBuf.ValueParameter>,
        addDefaultValue: Boolean = false
    ): List<FirValueParameter> {
        return valueParameters.map { proto ->
            val flags = if (proto.hasFlags()) proto.flags else 0
            val name = c.nameResolver.getName(proto.name)
            buildValueParameter {
                session = c.session
                returnTypeRef = proto.type(c.typeTable).toTypeRef(c)
                this.name = name
                symbol = FirVariableSymbol(name)
                defaultValue = defaultValue(flags)
                if (addDefaultValue) {
                    defaultValue = buildExpressionStub()
                }
                isCrossinline = Flags.IS_CROSSINLINE.get(flags)
                isNoinline = Flags.IS_NOINLINE.get(flags)
                isVararg = proto.varargElementType(c.typeTable) != null
                annotations += c.annotationDeserializer.loadValueParameterAnnotations(proto, c.nameResolver)
            }
        }.toList()
    }

    private fun ProtoBuf.Type.toTypeRef(context: FirDeserializationContext): FirTypeRef {
        return buildResolvedTypeRef {
            type = context.typeDeserializer.type(this@toTypeRef)
            annotations += context.annotationDeserializer.loadTypeAnnotations(this@toTypeRef, context.nameResolver)
        }
    }

}

