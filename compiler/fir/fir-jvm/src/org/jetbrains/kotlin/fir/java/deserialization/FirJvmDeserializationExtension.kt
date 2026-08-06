/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.deserialization.FirConstDeserializer
import org.jetbrains.kotlin.fir.deserialization.FirDeserializationExtension
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class FirJvmDeserializationExtension(session: FirSession) : FirDeserializationExtension(session) {
    override fun createConstDeserializer(
        containerSource: DeserializedContainerSource?,
        session: FirSession,
        serializerExtensionProtocol: SerializerExtensionProtocol,
    ): FirConstDeserializer? =
        if (containerSource is KotlinJvmBinarySourceElement)
            FirJvmConstDeserializer(containerSource.binaryClass, serializerExtensionProtocol)
        else
            null

    override fun FirRegularClassBuilder.configureDeserializedClass(classId: ClassId) {
        addSerializableIfNeeded(classId)
        addConstableSupertypesIfNeeded(classId)
    }

    private fun FirRegularClassBuilder.addSerializableIfNeeded(classId: ClassId) {
        if (this.status.isExpect) return
        if (!JvmBuiltInsSignatures.isSerializableInJava(classId.asSingleFqName().toUnsafe())) return
        superTypeRefs += resolvedSupertypeRef(JAVA_IO_SERIALIZABLE)
    }

    // On JDK 12 and later, the Java analogues of these builtin classes implement
    // java.lang.constant.Constable and java.lang.constant.ConstantDesc (KT-29858).
    // Whether the current JDK declares these interfaces is detected by resolving them
    // through the session's symbol provider, so `-jdk-release` is respected as well.
    private fun FirRegularClassBuilder.addConstableSupertypesIfNeeded(classId: ClassId) {
        if (this.status.isExpect) return
        if (!session.languageVersionSettings.supportsFeature(LanguageFeature.JvmBuiltinsConstableSupertypes)) return
        if (classId in CLASSES_WITH_CONSTABLE_SUPERTYPE && isConstableAvailable) {
            superTypeRefs += resolvedSupertypeRef(JvmStandardClassIds.Java.Constable)
        }
        if (classId in CLASSES_WITH_CONSTANT_DESC_SUPERTYPE && isConstantDescAvailable) {
            superTypeRefs += resolvedSupertypeRef(JvmStandardClassIds.Java.ConstantDesc)
        }
    }

    private val isConstableAvailable: Boolean by lazy {
        session.symbolProvider.getClassLikeSymbolByClassId(JvmStandardClassIds.Java.Constable) != null
    }

    private val isConstantDescAvailable: Boolean by lazy {
        session.symbolProvider.getClassLikeSymbolByClassId(JvmStandardClassIds.Java.ConstantDesc) != null
    }

    private fun resolvedSupertypeRef(classId: ClassId): FirResolvedTypeRef = buildResolvedTypeRef {
        coneType = ConeClassLikeTypeImpl(
            classId.toLookupTag(),
            typeArguments = ConeTypeProjection.EMPTY_ARRAY,
            isMarkedNullable = false
        )
    }

    override fun loadModuleName(classProto: ProtoBuf.Class, nameResolver: NameResolver): String? =
        classProto.getExtensionOrNull(JvmProtoBuf.classModuleName)?.let(nameResolver::getString)

    override fun loadHasBackingFieldFlag(propertyProto: ProtoBuf.Property): Boolean? =
        propertyProto.getExtensionOrNull(JvmProtoBuf.propertySignature)?.hasField()

    override fun isMaybeFullValueClass(containerSource: DeserializedContainerSource?): Boolean {
        val binaryClass = (containerSource as? KotlinJvmBinarySourceElement)?.binaryClass ?: return true
        // Since metadata version 2.4.0 annotations are stored in metadata,
        // so it is possible to distinguish full value classes from @JvmInline-based by the annotation
        return binaryClass.classHeader.metadataVersion.isAtLeast(2, 4, 0)
    }

    override val isLoadingOfAnnotationsOnAnnotationPropertiesEnabled: Boolean
        get() = session.languageVersionSettings.supportsFeature(LanguageFeature.JvmLoadAnnotationsOnAnnotationProperties)

    companion object {
        private val JAVA_IO_SERIALIZABLE = ClassId.topLevel(FqName("java.io.Serializable"))

        private val CLASSES_WITH_CONSTABLE_SUPERTYPE = setOf(
            StandardClassIds.Byte, StandardClassIds.Short, StandardClassIds.Int, StandardClassIds.Long,
            StandardClassIds.Float, StandardClassIds.Double, StandardClassIds.Char, StandardClassIds.Boolean,
            StandardClassIds.String, StandardClassIds.Enum,
        )

        private val CLASSES_WITH_CONSTANT_DESC_SUPERTYPE = setOf(
            StandardClassIds.Int, StandardClassIds.Long,
            StandardClassIds.Float, StandardClassIds.Double,
            StandardClassIds.String,
        )

        /**
         * Builtin classes whose deserialized supertypes depend on the SDK the use-site module is compiled against
         * (see [addConstableSupertypesIfNeeded]). Deserialized [FirRegularClass][org.jetbrains.kotlin.fir.declarations.FirRegularClass]
         * instances of these classes must not be shared between modules with different SDKs.
         */
        val CLASSES_WITH_SDK_DEPENDENT_SUPERTYPES: Set<ClassId> =
            CLASSES_WITH_CONSTABLE_SUPERTYPE + CLASSES_WITH_CONSTANT_DESC_SUPERTYPE
    }
}
