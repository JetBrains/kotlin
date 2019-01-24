/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.binaryTypeIsReference
import org.jetbrains.kotlin.backend.konan.isObjCClass
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.getFunctionalClassKind
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.DeserializedKonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.konanModuleOrigin
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isKFunction
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.serialization.konan.KonanPackageFragment
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty


/**
 * Implementation of given method.
 *
 * TODO: this method is actually a part of resolve and probably duplicates another one
 */
internal fun <T : CallableMemberDescriptor> T.resolveFakeOverride(allowAbstract: Boolean = false): T {
    if (this.kind.isReal) {
        return this
    } else {
        val overridden = OverridingUtil.getOverriddenDeclarations(this)
        val filtered = OverridingUtil.filterOutOverridden(overridden)
        // TODO: is it correct to take first?
        @Suppress("UNCHECKED_CAST")
        return filtered.first { allowAbstract || it.modality != Modality.ABSTRACT } as T
    }
}

internal val ClassDescriptor.isArray: Boolean
    get() = this.fqNameSafe.asString() in arrayTypes


internal val ClassDescriptor.isInterface: Boolean
    get() = (this.kind == ClassKind.INTERFACE)

private val kotlinNativeInternalPackageName = FqName.fromSegments(listOf("kotlin", "native", "internal"))

/**
 * @return `konan.internal` member scope
 */
internal val KonanBuiltIns.kotlinNativeInternal: MemberScope
    get() = this.builtInsModule.getPackage(kotlinNativeInternalPackageName).memberScope

internal val KotlinType.isKFunctionType: Boolean
    get() {
        val kind = constructor.declarationDescriptor?.getFunctionalClassKind()
        return kind == FunctionClassDescriptor.Kind.KFunction
    }

internal val FunctionDescriptor.isFunctionInvoke: Boolean
    get() {
        val dispatchReceiver = dispatchReceiverParameter ?: return false
        assert(!dispatchReceiver.type.isKFunctionType)

        return (dispatchReceiver.type.isFunctionType || dispatchReceiver.type.isSuspendFunctionType) &&
                this.isOperator && this.name == OperatorNameConventions.INVOKE
    }

internal val IrFunction.isFunctionInvoke: Boolean
    get() {
        val dispatchReceiver = dispatchReceiverParameter ?: return false
        assert(!dispatchReceiver.type.isKFunction())

        return dispatchReceiver.type.isFunction() &&
               /* this.isOperator &&*/ this.name == OperatorNameConventions.INVOKE
    }

internal fun ClassDescriptor.isUnit() = this.defaultType.isUnit()

internal fun ClassDescriptor.isNothing() = this.defaultType.isNothing()


internal val <T : CallableMemberDescriptor> T.allOverriddenDescriptors: List<T>
    get() {
        val result = mutableListOf<T>()
        fun traverse(descriptor: T) {
            result.add(descriptor)
            @Suppress("UNCHECKED_CAST")
            descriptor.overriddenDescriptors.forEach { traverse(it as T) }
        }
        traverse(this)
        return result
    }

internal val ClassDescriptor.contributedMethods: List<FunctionDescriptor>
    get () = unsubstitutedMemberScope.contributedMethods

internal val MemberScope.contributedMethods: List<FunctionDescriptor>
    get () {
        val contributedDescriptors = this.getContributedDescriptors()

        val functions = contributedDescriptors.filterIsInstance<FunctionDescriptor>()

        val properties = contributedDescriptors.filterIsInstance<PropertyDescriptor>()
        val getters = properties.mapNotNull { it.getter }
        val setters = properties.mapNotNull { it.setter }

        return functions + getters + setters
    }

fun ClassDescriptor.isAbstract() = this.modality == Modality.SEALED || this.modality == Modality.ABSTRACT
        || this.kind == ClassKind.ENUM_CLASS

internal val FunctionDescriptor.target: FunctionDescriptor
    get() = (if (modality == Modality.ABSTRACT) this else resolveFakeOverride()).original

tailrec internal fun DeclarationDescriptor.findPackage(): PackageFragmentDescriptor {
    return if (this is PackageFragmentDescriptor) this
    else this.containingDeclaration!!.findPackage()
}

internal fun DeclarationDescriptor.findPackageView(): PackageViewDescriptor {
    val packageFragment = this.findPackage()
    return packageFragment.module.getPackage(packageFragment.fqName)
}

internal fun DeclarationDescriptor.allContainingDeclarations(): List<DeclarationDescriptor> {
    var list = mutableListOf<DeclarationDescriptor>()
    var current = this.containingDeclaration
    while (current != null) {
        list.add(current)
        current = current.containingDeclaration
    }
    return list
}

// It is possible to declare "external inline fun",
// but it doesn't have much sense for native,
// since externals don't have IR bodies.
// Enforce inlining of constructors annotated with @InlineConstructor.

private val inlineConstructor = FqName("kotlin.native.internal.InlineConstructor")

internal val FunctionDescriptor.needsInlining: Boolean
    get() {
        val inlineConstructor = annotations.hasAnnotation(inlineConstructor)
        if (inlineConstructor) return true
        return (this.isInline && !this.isExternal)
    }

fun AnnotationDescriptor.getStringValueOrNull(name: String): String? {
    val constantValue = this.allValueArguments.entries.atMostOne {
        it.key.asString() == name
    }?.value
    return constantValue?.value as String?
}

fun <T> AnnotationDescriptor.getArgumentValueOrNull(name: String): T? {
    val constantValue = this.allValueArguments.entries.atMostOne {
        it.key.asString() == name
    }?.value
    return constantValue?.value as T?
}


fun AnnotationDescriptor.getStringValue(name: String): String = this.getStringValueOrNull(name)!!

private fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
    val result = mutableSetOf<FqName>()

    fun getSubPackages(fqName: FqName) {
        result.add(fqName)
        module.getSubPackagesOf(fqName) { true }.forEach { getSubPackages(it) }
    }

    getSubPackages(FqName.ROOT)
    return result
}

fun ModuleDescriptor.getPackageFragments(): List<PackageFragmentDescriptor> =
        getPackagesFqNames(this).flatMap {
            getPackage(it).fragments.filter { it.module == this }
        }

val ClassDescriptor.enumEntries: List<ClassDescriptor>
    get() {
        assert(this.kind == ClassKind.ENUM_CLASS)
        return this.unsubstitutedMemberScope.getContributedDescriptors()
                .filterIsInstance<ClassDescriptor>()
                .filter { it.kind == ClassKind.ENUM_ENTRY }
    }

internal val DeclarationDescriptor.isExpectMember: Boolean
    get() = this is MemberDescriptor && this.isExpect

internal val DeclarationDescriptor.isSerializableExpectClass: Boolean
    get() = this is ClassDescriptor && ExpectedActualDeclarationChecker.shouldGenerateExpectClass(this)

internal fun KotlinType?.createExtensionReceiver(owner: CallableDescriptor): ReceiverParameterDescriptor? =
        DescriptorFactory.createExtensionReceiverParameterForCallable(
                owner,
                this,
                Annotations.EMPTY
        )

internal fun FunctionDescriptorImpl.initialize(
        extensionReceiverType: KotlinType?,
        dispatchReceiverParameter: ReceiverParameterDescriptor?,
        typeParameters: List<TypeParameterDescriptor>,
        unsubstitutedValueParameters: List<ValueParameterDescriptor>,
        unsubstitutedReturnType: KotlinType?,
        modality: Modality?,
        visibility: Visibility
): FunctionDescriptorImpl = this.initialize(
        extensionReceiverType.createExtensionReceiver(this),
        dispatchReceiverParameter,
        typeParameters,
        unsubstitutedValueParameters,
        unsubstitutedReturnType,
        modality,
        visibility
)

private fun sourceByIndex(descriptor: CallableMemberDescriptor, index: Int): SourceFile {
    val fragment = descriptor.findPackage() as KonanPackageFragment
    return fragment.sourceFileMap.sourceFile(index)
}

fun CallableMemberDescriptor.findSourceFile(): SourceFile {
    val source = this.source.containingFile
    if (source != SourceFile.NO_SOURCE_FILE)
        return source
    return when {
        this is DeserializedSimpleFunctionDescriptor && proto.hasExtension(KonanProtoBuf.functionFile) -> sourceByIndex(
                this, proto.getExtension(KonanProtoBuf.functionFile))
        this is DeserializedPropertyDescriptor && proto.hasExtension(KonanProtoBuf.propertyFile) ->
            sourceByIndex(
                    this, proto.getExtension(KonanProtoBuf.propertyFile))
        else -> TODO()
    }
}

internal val TypedIntrinsic = FqName("kotlin.native.internal.TypedIntrinsic")
private val symbolNameAnnotation = FqName("kotlin.native.SymbolName")
private val objCMethodAnnotation = FqName("kotlinx.cinterop.ObjCMethod")
private val frozenAnnotation = FqName("kotlin.native.internal.Frozen")

internal val DeclarationDescriptor.isFrozen: Boolean
    get() = this.annotations.hasAnnotation(frozenAnnotation) ||
            (this is org.jetbrains.kotlin.descriptors.ClassDescriptor
                    // RTTI is used for non-reference type box or Objective-C object wrapper:
                    && (!this.defaultType.binaryTypeIsReference() || this.isObjCClass()))

internal val FunctionDescriptor.isTypedIntrinsic: Boolean
    get() = this.annotations.hasAnnotation(TypedIntrinsic)

// TODO: coalesce all our annotation value getters into fewer functions.
fun getAnnotationValue(annotation: AnnotationDescriptor): String? {
    return annotation.allValueArguments.values.ifNotEmpty {
        val stringValue = single() as? StringValue
        stringValue?.value
    }
}

fun CallableMemberDescriptor.externalSymbolOrThrow(): String? {
    this.annotations.findAnnotation(symbolNameAnnotation)?.let {
        return getAnnotationValue(it)!!
    }
    if (this.annotations.hasAnnotation(objCMethodAnnotation)) return null

    if (this.annotations.hasAnnotation(TypedIntrinsic)) return null

    if (this.annotations.hasAnnotation(RuntimeNames.cCall)) return null

    throw Error("external function ${this} must have @TypedIntrinsic, @SymbolName or @ObjCMethod annotation")
}

fun createAnnotation(
        descriptor: ClassDescriptor,
        vararg values: Pair<String, String>
): AnnotationDescriptor = AnnotationDescriptorImpl(
        descriptor.defaultType,
        values.map { (name, value) -> Name.identifier(name) to StringValue(value) }.toMap(),
        SourceElement.NO_SOURCE
)

val ModuleDescriptor.konanLibrary get() = (this.konanModuleOrigin as? DeserializedKonanModuleOrigin)?.library
