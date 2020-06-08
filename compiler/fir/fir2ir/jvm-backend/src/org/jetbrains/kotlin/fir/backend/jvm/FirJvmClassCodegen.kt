/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.fileParent
import org.jetbrains.kotlin.backend.jvm.codegen.mapClass
import org.jetbrains.kotlin.backend.jvm.lower.MultifileFacadeFileEntry
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.MultifileClassCodegenImpl
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.writeKotlinMetadata
import org.jetbrains.kotlin.codegen.writeSyntheticClassMetadata
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.inferenceContext
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.serialization.FirElementSerializer
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class FirJvmClassCodegen(
    irClass: IrClass,
    context: JvmBackendContext,
    parentFunction: IrFunction?,
    session: FirSession,
) : ClassCodegen(irClass, context, parentFunction) {
    private val serializerExtension = FirJvmSerializerExtension(session, visitor.serializationBindings, state, irClass, typeMapper)
    private val serializer: FirElementSerializer? =
        when (val metadata = irClass.metadata) {
            is FirMetadataSource.Class -> FirElementSerializer.create(
                metadata.klass, serializerExtension, (parentClassCodegen as? FirJvmClassCodegen)?.serializer
            )
            is FirMetadataSource.File -> FirElementSerializer.createTopLevel(session, serializerExtension)
            is FirMetadataSource.Function -> FirElementSerializer.createForLambda(session, serializerExtension)
            else -> null
        }

    private val approximator = object : AbstractTypeApproximator(session.inferenceContext) {
        override fun createErrorType(message: String): SimpleTypeMarker {
            return ConeKotlinErrorType(message)
        }
    }

    private fun FirTypeRef.approximated(
        toSuper: Boolean,
        conf: TypeApproximatorConfiguration = TypeApproximatorConfiguration.PublicDeclaration
    ): FirTypeRef {
        val approximatedType = if (toSuper)
            approximator.approximateToSuperType(this.coneTypeUnsafe(), conf)
        else
            approximator.approximateToSubType(this.coneTypeUnsafe(), conf)
        return withReplacedConeType(approximatedType as? ConeKotlinType)
    }

    private fun FirFunction<*>.copyToFreeAnonymousFunction(): FirAnonymousFunction {
        val function = this
        return buildAnonymousFunction {
            session = function.session
            origin = FirDeclarationOrigin.Source
            symbol = FirAnonymousFunctionSymbol()
            returnTypeRef = function.returnTypeRef.approximated(toSuper = true)
            receiverTypeRef = function.receiverTypeRef?.approximated(toSuper = false)
            isLambda = (function as? FirAnonymousFunction)?.isLambda == true
            valueParameters.addAll(function.valueParameters.map {
                buildValueParameterCopy(it) {
                    returnTypeRef = it.returnTypeRef.approximated(toSuper = false)
                }
            })
            typeParameters.addAll(function.typeParameters.filterIsInstance<FirTypeParameter>())
        }
    }

    @OptIn(DescriptorBasedIr::class)
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
                val classProto = serializer!!.classProto(metadata.klass).build()
                writeKotlinMetadata(visitor, state, KotlinClassHeader.Kind.CLASS, extraFlags) {
                    AsmUtil.writeAnnotationData(it, classProto, serializer.stringTable as JvmStringTable)
                }

                assert(irClass !in context.classNameOverride) {
                    "JvmPackageName is not supported for classes: ${irClass.render()}"
                }
            }
            is FirMetadataSource.File -> {
                val packageFqName = irClass.getPackageFragment()!!.fqName
                val packageProto = serializer!!.packagePartProto(packageFqName, metadata.file)

                serializerExtension.serializeJvmPackage(packageProto, type)

                val facadeClassName = context.multifileFacadeForPart[irClass.attributeOwnerId]
                val kind = if (facadeClassName != null) KotlinClassHeader.Kind.MULTIFILE_CLASS_PART else KotlinClassHeader.Kind.FILE_FACADE
                writeKotlinMetadata(visitor, state, kind, extraFlags) { av ->
                    AsmUtil.writeAnnotationData(av, packageProto.build(), serializer.stringTable as JvmStringTable)

                    if (facadeClassName != null) {
                        av.visit(JvmAnnotationNames.METADATA_MULTIFILE_CLASS_NAME_FIELD_NAME, facadeClassName.internalName)
                    }

                    if (irClass in context.classNameOverride) {
                        av.visit(JvmAnnotationNames.METADATA_PACKAGE_NAME_FIELD_NAME, irClass.fqNameWhenAvailable!!.parent().asString())
                    }
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
        val metadata = field.metadata
        if (metadata is FirMetadataSource.Property) {
            state.globalSerializationBindings.put(FirJvmSerializerExtension.FIELD_FOR_PROPERTY, metadata.property, fieldType to fieldName)
        }
    }
}