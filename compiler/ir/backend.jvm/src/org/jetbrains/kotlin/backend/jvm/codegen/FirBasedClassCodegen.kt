/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.MultifileFacadeFileEntry
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmSerializerExtension
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunction
import org.jetbrains.kotlin.fir.serialization.FirElementSerializer
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isFileClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class FirBasedClassCodegen internal constructor(
    irClass: IrClass,
    context: JvmBackendContext,
    parentFunction: IrFunction? = null
) : ClassCodegen(irClass, context, parentFunction) {

    private val session = (irClass.metadata as FirMetadataSource).session

    private val serializerExtension = FirJvmSerializerExtension(session, visitor.serializationBindings, state, typeMapper)
    private val serializer: FirElementSerializer? =
        when (val metadata = irClass.metadata) {
            is FirMetadataSource.Class -> FirElementSerializer.create(
                metadata.klass, serializerExtension, (parentClassCodegen as? FirBasedClassCodegen)?.serializer
            )
            is FirMetadataSource.Function -> FirElementSerializer.createForLambda(session, serializerExtension)
            else -> null
        }

    private fun FirFunction<*>.copyToFreeAnonymousFunction(): FirAnonymousFunction {
        val function = this
        return buildAnonymousFunction {
            session = function.session
            symbol = FirAnonymousFunctionSymbol()
            returnTypeRef = function.returnTypeRef
            receiverTypeRef = function.receiverTypeRef
            isLambda = false
            valueParameters.addAll(function.valueParameters)
            typeParameters.addAll(function.typeParameters.filterIsInstance<FirTypeParameter>())
        }
    }

    override fun generateKotlinMetadataAnnotation() {

        val localDelegatedProperties = (irClass.attributeOwnerId as? IrClass)?.let(context.localDelegatedProperties::get)
        if (localDelegatedProperties != null && localDelegatedProperties.isNotEmpty()) {
            state.bindingTrace.record(
                CodegenBinding.DELEGATED_PROPERTIES_WITH_METADATA, type, localDelegatedProperties.map { it.descriptor }
            )
        }

        // TODO: if `-Xmultifile-parts-inherit` is enabled, write the corresponding flag for parts and facades to [Metadata.extraInt].
        var extraFlags = JvmAnnotationNames.METADATA_JVM_IR_FLAG
        if (state.isIrWithStableAbi) {
            extraFlags += JvmAnnotationNames.METADATA_JVM_IR_STABLE_ABI_FLAG
        }

        when (val metadata = irClass.metadata) {
            is FirMetadataSource.Class -> {
                val classProto = serializer!!.classProto(irClass).build()
                writeKotlinMetadata(visitor, state, KotlinClassHeader.Kind.CLASS, extraFlags) {
                    AsmUtil.writeAnnotationData(it, classProto, serializer.stringTable as JvmStringTable)
                }

                assert(irClass !in context.classNameOverride) {
                    "JvmPackageName is not supported for classes: ${irClass.render()}"
                }
            }
            is FirMetadataSource.Function -> {
                val fakeAnonymousFunction = metadata.function.copyToFreeAnonymousFunction()
                val functionProto = serializer!!.functionProto(fakeAnonymousFunction)?.build()
                writeKotlinMetadata(visitor, state, KotlinClassHeader.Kind.SYNTHETIC_CLASS, extraFlags) {
                    if (functionProto != null) {
                        AsmUtil.writeAnnotationData(it, functionProto, serializer.stringTable as JvmStringTable)
                    }
                }
            }
            else -> {
                val entry = irClass.fileParent.fileEntry
                if (entry is MultifileFacadeFileEntry) {
                    val partInternalNames = entry.partFiles.mapNotNull { partFile ->
                        val fileClass = partFile.declarations.singleOrNull { it.isFileClass } as IrClass?
                        if (fileClass != null) typeMapper.mapClass(fileClass).internalName else null
                    }
                    MultifileClassCodegenImpl.writeMetadata(
                        visitor, state, extraFlags, partInternalNames, type, irClass.fqNameWhenAvailable!!.parent()
                    )
                } else {
                    writeSyntheticClassMetadata(visitor, state)
                }
            }
        }
    }

    override fun bindMethodMetadata(method: IrFunction, signature: Method) {
        when (val metadata = method.metadata) {
            is FirMetadataSource.Variable -> {
                // We can't check for JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS because for interface methods
                // moved to DefaultImpls, origin is changed to DEFAULT_IMPLS
                // TODO: fix origin somehow, because otherwise $annotations methods in interfaces also don't have ACC_SYNTHETIC
                assert(method.name.asString().endsWith(JvmAbi.ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX)) { method.dump() }

                state.globalSerializationBindings.put(
                    FirJvmSerializerExtension.SYNTHETIC_METHOD_FOR_FIR_VARIABLE, metadata.variable, signature
                )
            }
            is FirMetadataSource.Function -> {
                visitor.serializationBindings.put(FirJvmSerializerExtension.METHOD_FOR_FIR_FUNCTION, metadata.function, signature)
            }
            null -> {
            }
            else -> {
                error("Incorrect metadata source $metadata for:\n${method.dump()}")
            }
        }
    }

    override fun bindFieldMetadata(field: IrField, fieldType: Type, fieldName: String) {
        // TODO
    }
}