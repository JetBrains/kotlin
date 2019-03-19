/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirMemberFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.deserialization.AnnotatedCallableKind
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.types.Variance

class FirDeserializationContext(
    val nameResolver: NameResolver,
    val typeTable: TypeTable,
    val versionRequirementTable: VersionRequirementTable,
    val session: FirSession,
    val packageFqName: FqName,
    val relativeClassName: FqName?,
    val typeDeserializer: FirTypeDeserializer,
    val components: FirDeserializationComponents
) {
    fun childContext(
        typeParameterProtos: List<ProtoBuf.TypeParameter>
    ): FirDeserializationContext = FirDeserializationContext(
        nameResolver, typeTable, versionRequirementTable, session, packageFqName, relativeClassName,
        FirTypeDeserializer(
            nameResolver, typeTable, typeParameterProtos, typeDeserializer
        ),
        components
    )

    val memberDeserializer: FirMemberDeserializer = FirMemberDeserializer(this)

    companion object {
        fun createForPackage(
            fqName: FqName,
            packageProto: ProtoBuf.PackageFragment,
            nameResolver: NameResolver,
            session: FirSession
        ): FirDeserializationContext {
            val typeTable = TypeTable(packageProto.`package`.typeTable)
            return FirDeserializationContext(
                nameResolver, typeTable,
                VersionRequirementTable.EMPTY, // TODO:
                session,
                fqName,
                null,
                FirTypeDeserializer(
                    nameResolver,
                    typeTable,
                    emptyList(),
                    null
                ),
                FirDeserializationComponents()
            )
        }

        fun createForClass(
            classId: ClassId,
            classProto: ProtoBuf.Class,
            nameResolver: NameResolver,
            session: FirSession
        ): FirDeserializationContext {
            val classTypeTable = TypeTable(classProto.typeTable)
            return FirDeserializationContext(
                nameResolver, classTypeTable,
                VersionRequirementTable.EMPTY, // TODO:
                session,
                classId.packageFqName,
                classId.relativeClassName,
                FirTypeDeserializer(
                    nameResolver,
                    classTypeTable,
                    classProto.typeParameterList,
                    null
                ),
                FirDeserializationComponents()
            )
        }
    }
}

// TODO: Move something here
class FirDeserializationComponents

class FirMemberDeserializer(private val c: FirDeserializationContext) {
    private fun loadOldFlags(oldFlags: Int): Int {
        val lowSixBits = oldFlags and 0x3f
        val rest = (oldFlags shr 8) shl 6
        return lowSixBits + rest
    }

    fun loadFunction(proto: ProtoBuf.Function): FirNamedFunction {
        val flags = if (proto.hasFlags()) proto.flags else loadOldFlags(proto.oldFlags)

        val receiverAnnotations =
            // TODO: support annotations
            Annotations.EMPTY

        val versionRequirementTable =
            // TODO: Support case for KOTLIN_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME
            c.versionRequirementTable

        val callableName = c.nameResolver.getName(proto.name)
        val symbol = FirFunctionSymbol(CallableId(c.packageFqName, c.relativeClassName, callableName))
        val local = c.childContext(proto.typeParameterList)

        // TODO: support contracts
        return FirMemberFunctionImpl(
            c.session,
            null,
            symbol,
            callableName,
            ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags)),
            ProtoEnumFlags.modality(Flags.MODALITY.get(flags)),
            Flags.IS_EXPECT_FUNCTION.get(flags),
            false,
            false,
            Flags.IS_OPERATOR.get(flags),
            Flags.IS_INFIX.get(flags),
            Flags.IS_INLINE.get(flags),
            Flags.IS_TAILREC.get(flags),
            Flags.IS_EXTERNAL_FUNCTION.get(flags),
            Flags.IS_SUSPEND.get(flags),
            proto.receiverType(c.typeTable)?.let(local.typeDeserializer::type)?.toTypeRef(),
            local.typeDeserializer.type(proto.returnType(c.typeTable)).toTypeRef()
        ).apply {
            typeParameters += local.typeDeserializer.ownTypeParameters.map { createTypeParameterSymbol(it.name).fir }
            valueParameters += local.memberDeserializer.valueParameters(proto.valueParameterList)
            annotations += getAnnotations(proto, flags, AnnotatedCallableKind.FUNCTION)
        }
    }

    private fun createTypeParameterSymbol(name: Name): FirTypeParameterSymbol {
        val firSymbol = FirTypeParameterSymbol()
        FirTypeParameterImpl(c.session, null, firSymbol, name, variance = Variance.INVARIANT, isReified = false)
        return firSymbol
    }

    private fun valueParameters(
        valueParameters: List<ProtoBuf.ValueParameter>
    ): List<FirValueParameter> {
        return valueParameters.mapIndexed { i, proto ->
            val flags = if (proto.hasFlags()) proto.flags else 0
            FirValueParameterImpl(
                c.session, null, c.nameResolver.getName(proto.name),
                c.typeDeserializer.type(proto.type(c.typeTable)).toTypeRef(),
                null, // TODO: default value
                Flags.IS_CROSSINLINE.get(flags),
                Flags.IS_NOINLINE.get(flags),
                proto.varargElementType(c.typeTable) != null
            ).apply {
                annotations += emptyList() // TODO: parameter annotations
            }
        }.toList()
    }


    // TODO: Annotations
    private fun getAnnotations(proto: MessageLite, flags: Int, kind: AnnotatedCallableKind) = emptyList<FirAnnotationCall>()

    private fun ConeKotlinType.toTypeRef(): FirTypeRef {
        // TODO: annotations
        return FirResolvedTypeRefImpl(c.session, null, this, nullability.isNullable, emptyList())
    }

}

