/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

class JvmRuntimeTypes(
    module: ModuleDescriptor,
    private val languageVersionSettings: LanguageVersionSettings,
    private val generateOptimizedCallableReferenceSuperClasses: Boolean
) {
    private val kotlinJvmInternalPackage = MutablePackageFragmentDescriptor(module, FqName("kotlin.jvm.internal"))
    private val kotlinCoroutinesJvmInternalPackage =
        MutablePackageFragmentDescriptor(module, languageVersionSettings.coroutinesJvmInternalPackageFqName())

    private fun internal(className: String, packageFragment: PackageFragmentDescriptor = kotlinJvmInternalPackage): Lazy<ClassDescriptor> =
        lazy { createClass(packageFragment, className) }

    private fun coroutinesInternal(name: String): Lazy<ClassDescriptor> =
        lazy { createCoroutineSuperClass(name) }

    private fun propertyClasses(prefix: String, suffix: String): Lazy<List<ClassDescriptor>> =
        lazy { (0..2).map { i -> createClass(kotlinJvmInternalPackage, prefix + i + suffix) } }

    private val lambda: ClassDescriptor by internal("Lambda")

    private val functionReference: ClassDescriptor by internal("FunctionReference")
    val functionReferenceImpl: ClassDescriptor by internal("FunctionReferenceImpl")

    private val localVariableReference: ClassDescriptor by internal("LocalVariableReference")
    private val mutableLocalVariableReference: ClassDescriptor by internal("MutableLocalVariableReference")

    private val coroutineImpl: ClassDescriptor by internal("CoroutineImpl", kotlinCoroutinesJvmInternalPackage)
    private val continuationImpl: ClassDescriptor by coroutinesInternal("ContinuationImpl")
    private val restrictedContinuationImpl: ClassDescriptor by coroutinesInternal("RestrictedContinuationImpl")
    private val suspendLambda: ClassDescriptor by coroutinesInternal("SuspendLambda")
    private val restrictedSuspendLambda: ClassDescriptor by coroutinesInternal("RestrictedSuspendLambda")

    private val suspendFunctionInterface: ClassDescriptor? by lazy {
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

    private val propertyReferences: List<ClassDescriptor> by propertyClasses("PropertyReference", "")
    private val mutablePropertyReferences: List<ClassDescriptor> by propertyClasses("MutablePropertyReference", "")
    private val propertyReferenceImpls: List<ClassDescriptor> by propertyClasses("PropertyReference", "Impl")
    private val mutablePropertyReferenceImpls: List<ClassDescriptor> by propertyClasses("MutablePropertyReference", "Impl")

    private fun createClass(
        packageFragment: PackageFragmentDescriptor,
        name: String,
        classKind: ClassKind = ClassKind.CLASS
    ): ClassDescriptor =
        MutableClassDescriptor(
            packageFragment, classKind, false, false, Name.identifier(name), SourceElement.NO_SOURCE, LockBasedStorageManager.NO_LOCKS
        ).apply {
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
                if (actualFunctionDescriptor.extensionReceiverParameter?.type
                        ?.isRestrictsSuspensionReceiver(languageVersionSettings) == true
                ) {
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
        val superClass = if (generateOptimizedCallableReferenceSuperClasses) functionReferenceImpl else functionReference
        return listOfNotNull(superClass.defaultType, functionType, suspendFunctionType)
    }

    fun getSupertypeForPropertyReference(descriptor: VariableDescriptorWithAccessors, isMutable: Boolean, isBound: Boolean): KotlinType {
        if (descriptor is LocalVariableDescriptor) {
            return (if (isMutable) mutableLocalVariableReference else localVariableReference).defaultType
        }

        val arity =
            (if (descriptor.extensionReceiverParameter != null) 1 else 0) +
                    (if (descriptor.dispatchReceiverParameter != null) 1 else 0) -
                    if (isBound) 1 else 0

        val classes = when {
            generateOptimizedCallableReferenceSuperClasses -> if (isMutable) mutablePropertyReferenceImpls else propertyReferenceImpls
            else -> if (isMutable) mutablePropertyReferences else propertyReferences
        }

        return classes[arity].defaultType
    }
}
