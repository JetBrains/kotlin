/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendExtension
import org.jetbrains.kotlin.backend.jvm.ModuleMetadataSerializer
import org.jetbrains.kotlin.backend.jvm.metadata.BuiltinsSerializer
import org.jetbrains.kotlin.backend.jvm.metadata.MetadataSerializer
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.serialization.FirElementAwareStringTable
import org.jetbrains.kotlin.fir.serialization.FirElementSerializer
import org.jetbrains.kotlin.fir.serialization.TypeApproximatorForMetadataSerializer
import org.jetbrains.kotlin.fir.serialization.serializeAnnotations
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.org.objectweb.asm.Type

class FirJvmBackendExtension(
    private val components: Fir2IrComponents,
    private val actualizedExpectDeclarations: Set<FirDeclaration>?
) : JvmBackendExtension {
    override fun createSerializer(
        context: JvmBackendContext,
        klass: IrClass,
        type: Type,
        bindings: JvmSerializationBindings,
        parentSerializer: MetadataSerializer?
    ): MetadataSerializer {
        return makeFirMetadataSerializerForIrClass(
            components.session,
            context,
            klass,
            bindings,
            components,
            parentSerializer,
            actualizedExpectDeclarations
        )
    }

    override fun createModuleMetadataSerializer(context: JvmBackendContext): ModuleMetadataSerializer = object : ModuleMetadataSerializer {
        override fun serializeOptionalAnnotationClass(metadata: MetadataSource.Class, stringTable: StringTableImpl): ProtoBuf.Class {

            require(metadata is FirMetadataSource.Class) { "Metadata is expected to be ${FirMetadataSource.Class::class.simpleName}" }

            val session = components.session
            val fir = metadata.fir

            val typeApproximator = TypeApproximatorForMetadataSerializer(session)
            val firSerializerExtension = object : FirJvmSerializerExtension(
                session,
                JvmSerializationBindings(),
                context.state,
                // annotation can't have local delegated properties, it is safe to pass empty list
                localDelegatedProperties = emptyList(),
                typeApproximator,
                components,
                object : FirElementAwareStringTable {
                    override fun getQualifiedClassNameIndex(className: String, isLocal: Boolean): Int =
                        stringTable.getQualifiedClassNameIndex(className, isLocal)

                    override fun getStringIndex(string: String): Int = stringTable.getStringIndex(string)

                    override fun getPackageFqNameIndexByString(fqName: String): Int = stringTable.getPackageFqNameIndexByString(fqName)
                }
            ) {
                override val isOptionalAnnotationClassSerialization: Boolean
                    get() = true

                override fun serializeClass(
                    klass: FirClass,
                    proto: ProtoBuf.Class.Builder,
                    versionRequirementTable: MutableVersionRequirementTable,
                    childSerializer: FirElementSerializer,
                ) {
                    // Before 2.2, annotations were written to the `BuiltInsProtoBuf.classAnnotation` extension.
                    // Starting from 2.2, they are written to the `ProtoBuf.Class.annotation` field (KT-57919).
                    if (!context.config.metadataVersion.isAtLeast(2, 2, 0)) {
                        klass.serializeAnnotations(
                            session,
                            additionalMetadataProvider,
                            annotationSerializer,
                            proto,
                            BuiltInSerializerProtocol.classAnnotation
                        )
                    }
                    super.serializeClass(klass, proto, versionRequirementTable, childSerializer)
                }
            }
            val serializer = FirElementSerializer.create(
                session,
                components.scopeSession,
                fir,
                firSerializerExtension,
                parentSerializer = null,
                typeApproximator,
                context.config.languageVersionSettings
            )
            val file = session.firProvider.getFirClassifierContainerFileIfAny(fir.symbol)
            return serializer.classProto(fir, file).build()
        }
    }

    override fun createBuiltinsSerializer(): BuiltinsSerializer = FirBuiltInsSerializer(components.session, components.scopeSession)
}
