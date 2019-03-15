/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.codegen.coroutines.coroutinesJvmInternalPackageFqName
import org.jetbrains.kotlin.codegen.coroutines.getOrCreateJvmSuspendFunctionView
import org.jetbrains.kotlin.codegen.coroutines.isSuspendLambdaOrLocalFunction
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.MutableClassDescriptor
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.checkers.isRestrictsSuspensionReceiver
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType

class JvmRuntimeTypes(module: ModuleDescriptor, private val languageVersionSettings: LanguageVersionSettings) {
    private val kotlinJvmInternalPackage = MutablePackageFragmentDescriptor(module, FqName("kotlin.jvm.internal"))
    private val kotlinCoroutinesJvmInternalPackage =
        MutablePackageFragmentDescriptor(module, languageVersionSettings.coroutinesJvmInternalPackageFqName())

    private fun klass(name: String) = lazy { createClass(kotlinJvmInternalPackage, name) }

    private val lambda: ClassDescriptor by klass("Lambda")
    val functionReference: ClassDescriptor by klass("FunctionReference")
    private val localVariableReference: ClassDescriptor by klass("LocalVariableReference")
    private val mutableLocalVariableReference: ClassDescriptor by klass("MutableLocalVariableReference")

    private val coroutineImpl: ClassDescriptor by lazy {
        createClass(kotlinCoroutinesJvmInternalPackage, "CoroutineImpl")
    }

    private val continuationImpl by lazy {
        createCoroutineSuperClass("ContinuationImpl")
    }

    private val restrictedContinuationImpl by lazy {
        createCoroutineSuperClass("RestrictedContinuationImpl")
    }

    private val suspendLambda by lazy {
        createCoroutineSuperClass("SuspendLambda")
    }

    private val restrictedSuspendLambda by lazy {
        createCoroutineSuperClass("RestrictedSuspendLambda")
    }

    private val suspendFunctionInterface by lazy {
        if (languageVersionSettings.isReleaseCoroutines())
            createClass(kotlinCoroutinesJvmInternalPackage, "SuspendFunction", ClassKind.INTERFACE)
        else null
    }

    private fun createCoroutineSuperClass(className: String): ClassDescriptor {
        return if (languageVersionSettings.isReleaseCoroutines())
            createClass(kotlinCoroutinesJvmInternalPackage, className)
        else
            coroutineImpl
    }

    private val propertyReferences: List<ClassDescriptor> by lazy {
        (0..2).map { i -> createClass(kotlinJvmInternalPackage, "PropertyReference$i") }
    }

    private val mutablePropertyReferences: List<ClassDescriptor> by lazy {
        (0..2).map { i -> createClass(kotlinJvmInternalPackage, "MutablePropertyReference$i") }
    }

    private fun createClass(
            packageFragment: PackageFragmentDescriptor,
            name: String,
            classKind: ClassKind = ClassKind.CLASS
    ): ClassDescriptor =
            MutableClassDescriptor(packageFragment, classKind, /* isInner = */ false, /* isExternal = */ false,
                                   Name.identifier(name), SourceElement.NO_SOURCE, LockBasedStorageManager.NO_LOCKS).apply {
                modality = Modality.FINAL
                visibility = Visibilities.PUBLIC
                setTypeParameterDescriptors(emptyList())
                createTypeConstructor()
            }

    fun getSupertypesForClosure(descriptor: FunctionDescriptor): Collection<KotlinType> {

        val actualFunctionDescriptor =
            if (descriptor.isSuspend)
                getOrCreateJvmSuspendFunctionView(descriptor, languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines))
            else
                descriptor

        val functionType = createFunctionType(
                descriptor.builtIns,
                Annotations.EMPTY,
                actualFunctionDescriptor.extensionReceiverParameter?.type,
                actualFunctionDescriptor.valueParameters.map { it.type },
                null,
                actualFunctionDescriptor.returnType!!
        )

        if (descriptor.isSuspend) {
            return mutableListOf<KotlinType>().apply {
                if (actualFunctionDescriptor.extensionReceiverParameter?.type?.isRestrictsSuspensionReceiver(languageVersionSettings) == true) {
                    if (descriptor.isSuspendLambdaOrLocalFunction()) {
                        add(restrictedSuspendLambda.defaultType)
                    } else {
                        add(restrictedContinuationImpl.defaultType)
                    }
                } else {
                    if (descriptor.isSuspendLambdaOrLocalFunction()) {
                        add(suspendLambda.defaultType)
                    } else {
                        add(continuationImpl.defaultType)
                    }
                }

                if (descriptor.isSuspendLambdaOrLocalFunction()) {
                    add(functionType)
                }
            }
        }

        return listOf(lambda.defaultType, functionType)
    }

    fun getSupertypesForFunctionReference(
            referencedFunction: FunctionDescriptor,
            anonymousFunctionDescriptor: AnonymousFunctionDescriptor,
            isBound: Boolean
    ): Collection<KotlinType> {
        val receivers = computeExpectedNumberOfReceivers(referencedFunction, isBound)

        val functionType = createFunctionType(
            referencedFunction.builtIns,
            Annotations.EMPTY,
            if (isBound) null else referencedFunction.extensionReceiverParameter?.type
                    ?: referencedFunction.dispatchReceiverParameter?.type,
            anonymousFunctionDescriptor.valueParameters.drop(receivers).map { it.type },
            null,
            referencedFunction.returnType!!,
            referencedFunction.isSuspend
        )

        val suspendFunctionType = if (referencedFunction.isSuspend) suspendFunctionInterface?.defaultType else null
        return listOfNotNull(functionReference.defaultType, functionType, suspendFunctionType)
    }

    fun getSupertypeForPropertyReference(descriptor: VariableDescriptorWithAccessors, isMutable: Boolean, isBound: Boolean): KotlinType {
        if (descriptor is LocalVariableDescriptor) {
            return (if (isMutable) mutableLocalVariableReference else localVariableReference).defaultType
        }

        val arity =
                (if (descriptor.extensionReceiverParameter != null) 1 else 0) +
                (if (descriptor.dispatchReceiverParameter != null) 1 else 0) -
                if (isBound) 1 else 0

        return (if (isMutable) mutablePropertyReferences else propertyReferences)[arity].defaultType
    }
}
