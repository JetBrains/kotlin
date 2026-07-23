/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.jklib

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrMangleComputer
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.FilteredByPredicateAnnotations
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.jvm.serialization.BaseJvmIrMangler
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.typeEnhancement.hasEnhancedNullability
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations
import org.jetbrains.kotlin.utils.DFS.ifAny as ifAnyDFS
import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideMaybeAbstract
import org.jetbrains.kotlin.name.FqName

/*
 * The manglers defined in this file compute plain JVM signatures for Java methods.
 * For example instead of this:
 *  ```
 *  java/lang/Comparator.thenComparing(java.util.function.Function<in|1:0?,out|0:0?>?){0§<kotlin.Comparable<in|0:0?>?>}
 *  ```
 *  We will compute:
 *  ```
 *  Ljava/util/Comparator.thenComparing(Ljava/util/function/Function;)Ljava/util/Comparator;
 *  ```
 * This logic aims to fix signature differences between K1 and K2.
 * TODO(KT-81659): When the klib deserialization part transitions to K2, code in this file should no longer be needed and we could
 * directly reuse manglers from Kotlin/JVM.
 */

class JKlibIrMangler : BaseJvmIrMangler() {
  private val generatedSignatures = ConcurrentHashMap<IrClass, MutableMap<String, IrDeclaration>>()

  override fun IrDeclaration.signatureMangle(compatibleMode: Boolean): Long {
    return signatureString(compatibleMode).hashMangle
  }

  @OptIn(ObsoleteDescriptorBasedAPI::class)
  private fun IrDeclaration.signatureStringImpl(compatibleMode: Boolean): String {
    val target =
      if (this is IrOverridableDeclaration<*> && isFakeOverride && !this.shouldUseJvmSignature()) {
        resolveFakeOverrideMaybeAbstract() as? IrDeclaration ?: this
      } else {
        this
      }

    var result: String? = null
    if (this.shouldUseJvmSignature()) {
      val jvmSig = (target.descriptor as? CallableDescriptor)?.computeJvmSignatureSafe()
      if (jvmSig != null) {
        result = jvmSig
      } else {
        result = getMangleComputer(MangleMode.SIGNATURE, compatibleMode).computeMangle(target)
      }
    } else if (
      this is IrProperty &&
        getter == null &&
        setter == null &&
        (isFakeOverride || backingField == null)
    ) {
      val parentSig = (parent as? IrDeclaration)?.signatureString(compatibleMode)
      if (parentSig != null) {
        result = "$parentSig.${name.asString()}"
      }
    }

    if (result == null) {
      result = getMangleComputer(MangleMode.SIGNATURE, compatibleMode).computeMangle(this)
    }
    return result
  }

  @OptIn(ObsoleteDescriptorBasedAPI::class)
  override fun IrDeclaration.signatureString(compatibleMode: Boolean): String {
    val sig = signatureStringImpl(compatibleMode)
    if (this is IrOverridableDeclaration<*> && isFakeOverride) {
      val parentClass = parent as? IrClass
      if (parentClass != null) {
        val classMap = generatedSignatures.getOrPut(parentClass) { ConcurrentHashMap() }
        var uniqueSig = sig
        var counter = 0
        while (true) {
          val existing = classMap.putIfAbsent(uniqueSig, this)
          if (existing == null || existing == this) {
            return uniqueSig
          }
          // Clash! Try next unique signature
          uniqueSig = "$sig#${counter++}"
        }
      }
    }
    return sig
  }

    private class JKlibIrManglerComputer(builder: StringBuilder, mode: MangleMode, compatibleMode: Boolean) :
        JvmIrManglerComputer(builder, mode, compatibleMode, useEffectiveTypeVariances = true) {
        override fun copy(newMode: MangleMode): IrMangleComputer =
            JKlibIrManglerComputer(builder, newMode, compatibleMode)

        override fun addReturnTypeSpecialCase(function: IrFunction): Boolean = false

        override fun mangleTypePlatformSpecific(type: IrType, tBuilder: StringBuilder) {
            if (type.hasAnnotation(JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION)) {
                tBuilder.append(MangleConstant.ENHANCED_NULLABILITY_MARK)
            }
        }
    }

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<IrDeclaration> =
        JKlibIrManglerComputer(StringBuilder(256), mode, compatibleMode)
}

class JKlibDescriptorMangler(private val mainDetector: MainFunctionDetector?) : JvmDescriptorMangler(mainDetector) {

  override fun DeclarationDescriptor.signatureString(compatibleMode: Boolean): String {
    return if (shouldUseJvmSignature()) {
      val target = this
      (target as? CallableDescriptor)?.computeJvmSignatureSafe()
        ?: getMangleComputer(MangleMode.SIGNATURE, compatibleMode).computeMangle(target)
    } else {
      getMangleComputer(MangleMode.SIGNATURE, compatibleMode).computeMangle(this)
    }
  }

    private class JKlibDescriptorManglerComputer(
        builder: StringBuilder,
        private val mainDetector: MainFunctionDetector?,
        mode: MangleMode,
    ) : JvmDescriptorManglerComputer(builder, mainDetector, mode, useEffectiveTypeVariances = true) {
        override fun addReturnTypeSpecialCase(function: FunctionDescriptor): Boolean = false

        override fun copy(newMode: MangleMode): DescriptorMangleComputer = JKlibDescriptorManglerComputer(builder, mainDetector, newMode)
    }

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<DeclarationDescriptor> =
        JKlibDescriptorManglerComputer(StringBuilder(256), mainDetector, mode)
}

private fun String.isKotlinPackage(): Boolean {
    return this == "kotlin" || startsWith("kotlin.")
}

private fun StringBuilder.appendErasedType(type: KotlinType) {
    append(type.mapToJvmType())
}

private fun KotlinType.mapToJvmType(): JvmType {
    var type = this
    // Under JSpecify strict mode, Kotlinc loads non-nullable enhanced Java boxed types (like `@NonNull Integer` or `@NonNull Boolean`) as
    // non-nullable Kotlin primitive types (`kotlin.Int`, `kotlin.Boolean`) with a `@EnhancedNullability` annotation.
    //
    // However, K2's IR-backed descriptors used during Klib serialization lose this `@EnhancedNullability` annotation. Consequently, the
    // `mapType()` call below maps them to primitive JVM types (`I`, `Z`) in Klib metadata signatures instead of boxed types.
    //
    // The Klib deserialization process using K1 does not have this issue and correctly maps the type leading to some signature mismatches
    // during the IR linking.
    //
    // To resolve this discrepancy, we strip the `hasEnhancedNullability` annotations in J2CL's mangler. This matches K2's "stripped"
    // behavior, forcing `mapType` to map them to primitive descriptors (`I`, `Z`) to ensure they link successfully. True Java primitives
    // (which never had `@EnhancedNullability`) are unaffected.
    // TODO(KT-86165): A proper long-term solution would be to avoid using IrBasedDescriptors at all in K2 and compute the JVM signature
    //  from the IR.  The current workaround will fail if a Java class defines overloads with both non-nullable boxed and primitive types,
    //  as both will be assigned the same JVM signature:
    //  ```
    //  class A {
    //    public void foo(@NonNull Integer x) { ... }
    //    public void foo(int x) { ... }
    //  }
    //  ```
    //  Both overloads are mapped to `foo(I)V` and may link incorrectly.
    if (
        type is SimpleType &&
        KotlinBuiltIns.isPrimitiveType(type) &&
        !type.isNullable() &&
        type.hasEnhancedNullability()
    ) {
        type =
            type.replaceAnnotations(FilteredByPredicateAnnotations(type.annotations) { JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION != it.fqName })
    }

    return mapType(
        type,
        JvmTypeFactoryImpl,
        TypeMappingMode.DEFAULT,
        TypeMappingConfigurationImpl,
        descriptorTypeWriter = null,
    )
}

private fun hasVoidReturnType(descriptor: CallableDescriptor): Boolean {
    if (descriptor is ConstructorDescriptor) return true
    return KotlinBuiltIns.isUnit(descriptor.returnType!!) && !TypeUtils.isNullableType(descriptor.returnType!!)
            && descriptor !is PropertyGetterDescriptor
}

private fun FunctionDescriptor.computeJvmDescriptor(withReturnType: Boolean = true, withName: Boolean = true): String = buildString {
    if (withName) {
        append(if (this@computeJvmDescriptor is ConstructorDescriptor) "<init>" else name.asString())
    }

    append("(")

    extensionReceiverParameter?.let {
        appendErasedType(it.type)
    }

    for (parameter in valueParameters) {
        appendErasedType(parameter.type)
    }

    append(")")

    if (withReturnType) {
        if (hasVoidReturnType(this@computeJvmDescriptor)) {
            append("V")
        } else {
            appendErasedType(returnType!!)
        }
    }
}

private fun KotlinType.isLocalOrAnonymous(): Boolean {
    val descriptor = constructor.declarationDescriptor ?: return false
    return descriptor is ClassDescriptor && (DescriptorUtils.isLocal(descriptor) || DescriptorUtils.isAnonymousObject(descriptor))
}

private fun CallableDescriptor.computeJvmSignature(): String? = signatures {
    if (DescriptorUtils.isLocal(this@computeJvmSignature)) return null

    val classDescriptor = containingDeclaration as? ClassDescriptor ?: return null
    if (classDescriptor.name.isSpecial) return null
    
    val function = original as? FunctionDescriptor ?: return null
    if (function.extensionReceiverParameter?.type?.isLocalOrAnonymous() == true) return null
    if (function.valueParameters.any { it.type.isLocalOrAnonymous() }) return null
    if (function.returnType?.isLocalOrAnonymous() == true) return null

    signature(
        classDescriptor,
        function.computeJvmDescriptor()
    )
}

// TODO(KT-84880): Replace with IR-based signature computation (MethodSignatureMapper?) 
private fun CallableDescriptor.computeJvmSignatureSafe(): String? {
    return try {
        computeJvmSignature()
    } catch (e: Exception) {
        Logger.getInstance(JKlibIrMangler::class.java).error("Failed to compute JVM signature for $this", e)
        null
    }
}

private fun IrDeclaration.isDeclaredInJava(): Boolean {
    if (origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) return true
    val ownerClass = parentClassOrNull
    return ownerClass?.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
}

private fun DeclarationDescriptor.isDeclaredInJava(): Boolean =
  this is JavaCallableMemberDescriptor || containingDeclaration is JavaClassDescriptor

private fun IrDeclaration.isMappedJreClassMember(): Boolean {
  val parentClass = parent as? IrClass ?: return false
  val fqName = parentClass.classId?.asSingleFqName() ?: return false
  val mappedFqName = JavaToKotlinClassMap.mapKotlinToJava(fqName.toUnsafe())?.asSingleFqName() ?: return false
  return !mappedFqName.asString().isKotlinPackage()
}

private fun DeclarationDescriptor.isMappedJreClassMember(): Boolean {
  val parentClass = containingDeclaration as? ClassDescriptor ?: return false
  val fqName = parentClass.fqNameOrNull() ?: return false
  val mappedFqName = JavaToKotlinClassMap.mapKotlinToJava(fqName.toUnsafe())?.asSingleFqName() ?: return false
  return !mappedFqName.asString().isKotlinPackage()
}

private fun IrOverridableDeclaration<*>.hasMappedJreClassInOverrideChain(): Boolean {
  val visited = mutableSetOf<IrOverridableDeclaration<*>>()
  fun dfs(decl: IrOverridableDeclaration<*>): Boolean {
    if (!visited.add(decl)) return false
    val irDecl = decl as? IrDeclaration ?: return false
    if ((irDecl.isDeclaredInJava() || irDecl.isMappedJreClassMember()) && !irDecl.isContainingClassKotlinOnly()) {
      return true
    }
    for (symbol in decl.overriddenSymbols) {
      val owner = symbol.owner as? IrOverridableDeclaration<*> ?: continue
      if (dfs(owner)) return true
    }
    return false
  }
  return dfs(this)
}

private fun CallableMemberDescriptor.hasMappedJreClassInOverrideChain(): Boolean {
  val visited = mutableSetOf<CallableMemberDescriptor>()
  fun dfs(desc: CallableMemberDescriptor): Boolean {
    if (!visited.add(desc)) return false
    if ((desc.isDeclaredInJava() || desc.isMappedJreClassMember()) && !desc.isContainingClassKotlinOnly()) {
      return true
    }
    for (overridden in desc.overriddenDescriptors) {
      if (dfs(overridden)) return true
    }
    return false
  }
  return dfs(this)
}

private val jvmBackedKotlinClasses = setOf(
    "Number", "Enum", "Array",
    "Boolean", "Byte", "Short", "Int", "Long", "Float", "Double", "Char",
    "UByte", "UShort", "UInt", "ULong"
)

private fun isKotlinOnlyClass(clazz: IrClass): Boolean {
  val fqName = clazz.classId?.asSingleFqName() ?: return false
  val packageName = fqName.parent().asString()
  if (packageName == "kotlin") {
    val className = fqName.shortName().asString()
    if (className in jvmBackedKotlinClasses || className.endsWith("Array")) {
      return false
    }
    return true
  }
  return packageName.startsWith("kotlin.") && clazz.kind == ClassKind.INTERFACE
}

private fun isKotlinOnlyClass(clazz: ClassDescriptor): Boolean {
  val fqName = clazz.fqNameOrNull() ?: return false
  val packageName = fqName.parent().asString()
  if (packageName == "kotlin") {
    val className = fqName.shortName().asString()
    if (className in jvmBackedKotlinClasses || className.endsWith("Array")) {
      return false
    }
    return true
  }
  return packageName.startsWith("kotlin.") && clazz.kind == ClassKind.INTERFACE
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
private fun IrDeclaration.shouldUseJvmSignature(): Boolean {
  if (isContainingClassKotlinOnly()) return false
  if (overridesRealJavaMember()) return true
  val overridable = this as? IrOverridableDeclaration<*>
  val roots = overridable?.findRoots() ?: emptySet()
  val isJavaBacked =
    isDeclaredInJava() ||
      isMappedJreClassMember() ||
      roots.any {
        (it as? IrDeclaration)?.isDeclaredInJava() == true ||
          (it as? IrDeclaration)?.isMappedJreClassMember() == true
      }
  if (!isJavaBacked) return false
  if (getPackageFragment().packageFqName.asString().isKotlinPackage() && !isMappedJreClassMember())
    return false
  if (overridable != null && hasKotlinOnlyRootIr(roots) && !overridable.hasMappedJreClassInOverrideChain()) return false
  if (this is IrFunction) {
    if (origin == IrDeclarationOrigin.BRIDGE || origin == IrDeclarationOrigin.BRIDGE_SPECIAL)
      return false
    if (origin.isSynthetic && !isEnumSyntheticMethod()) return false
  }
  if (this is IrSimpleFunction && isFakeOverride) {
    val parentClass = parentClassOrNull
    val thisSig = (descriptor as? CallableDescriptor)?.computeJvmSignatureSafe()
    val realFunctions = mutableListOf<IrSimpleFunction>()
    parentClass?.declarations?.forEach { decl ->
      when (decl) {
        is IrSimpleFunction -> if (!decl.isFakeOverride) realFunctions.add(decl)
        is IrProperty -> if (!decl.isFakeOverride) {
          decl.getter?.let { realFunctions.add(it) }
          decl.setter?.let { realFunctions.add(it) }
        }
      }
    }
    val hasRealClashingDeclaration =
      realFunctions.any {
        it.dispatchReceiverParameter != null &&
          (it.descriptor as? CallableDescriptor)?.computeJvmSignatureSafe() == thisSig
      }
    if (hasRealClashingDeclaration) return false
  }
  if (this is IrProperty && isFakeOverride) {
    return false
  }
  return true
}

private fun hasKotlinOnlyRootIr(roots: Set<IrOverridableDeclaration<*>>): Boolean {
  return roots.any { root ->
    val parentClass = (root as? IrDeclaration)?.parent as? IrClass
    parentClass != null && isKotlinOnlyClass(parentClass)
  }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
private fun IrOverridableDeclaration<*>.findRoots(): Set<IrOverridableDeclaration<*>> {
  if (overriddenSymbols.isEmpty()) return setOf(this)
  val roots = mutableSetOf<IrOverridableDeclaration<*>>()
  val visited = mutableSetOf<IrOverridableDeclaration<*>>()
  fun dfs(decl: IrOverridableDeclaration<*>) {
    if (!visited.add(decl)) return
    if (decl.overriddenSymbols.isEmpty()) roots.add(decl)
    else decl.overriddenSymbols.forEach { (it.owner as? IrOverridableDeclaration<*>)?.let(::dfs) }
  }
  dfs(this)
  return roots
}

private fun IrDeclaration.isEnumSyntheticMethod(): Boolean =
  this is IrFunction &&
    (parent as? IrClass)?.kind == ClassKind.ENUM_CLASS &&
    (name.asString() == "values" || name.asString() == "valueOf")

private fun DeclarationDescriptor.shouldUseJvmSignature(): Boolean {
  if (isContainingClassKotlinOnly()) return false
  if (overridesRealJavaMember()) return true
  val callableMember = this as? CallableMemberDescriptor
  val roots = callableMember?.findRoots() ?: emptySet()
  val isJavaBacked =
    isDeclaredInJava() ||
      isMappedJreClassMember() ||
      roots.any { it.isDeclaredInJava() || it.isMappedJreClassMember() }
  if (!isJavaBacked) return false
  if (containingPackage()?.asString()?.isKotlinPackage() == true && !isMappedJreClassMember())
    return false
  if (callableMember != null && hasKotlinOnlyRootDesc(roots) && !callableMember.hasMappedJreClassInOverrideChain()) return false
  if (this is IrBasedDeclarationDescriptor<*>) {
    return !owner.origin.isSynthetic || owner.isEnumSyntheticMethod()
  }
  if (this is CallableMemberDescriptor) {
    if (kind == CallableMemberDescriptor.Kind.SYNTHESIZED && !isEnumSyntheticMethod()) return false
    if (kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
      if (this is PropertyDescriptor) return false
      val containingClass = containingDeclaration as? ClassDescriptor
      val thisSig = this.computeJvmSignatureSafe()
      val hasRealClashingDeclaration =
        containingClass
          ?.unsubstitutedMemberScope
          ?.getContributedFunctions(
            name,
            org.jetbrains.kotlin.incremental.components.NoLookupLocation.FROM_BACKEND,
          )
          ?.any {
            it.kind.isReal &&
              it.dispatchReceiverParameter != null &&
              it.computeJvmSignatureSafe() == thisSig
          } ?: false
      if (hasRealClashingDeclaration) return false

      val hasRealClashingProperty =
        containingClass
          ?.unsubstitutedMemberScope
          ?.getContributedDescriptors(DescriptorKindFilter.VARIABLES)
          ?.filterIsInstance<PropertyDescriptor>()
          ?.any { property ->
            property.kind.isReal &&
              property.dispatchReceiverParameter != null &&
              (property.getter?.computeJvmSignatureSafe() == thisSig ||
               property.setter?.computeJvmSignatureSafe() == thisSig)
          } ?: false
      if (hasRealClashingProperty) return false
    }
  }
  return true
}

private fun hasKotlinOnlyRootDesc(roots: Set<CallableMemberDescriptor>): Boolean =
  roots.any { root ->
    val parentClass = root.containingDeclaration as? ClassDescriptor
    parentClass != null && isKotlinOnlyClass(parentClass)
  }

private fun CallableMemberDescriptor.findRoots(): Set<CallableMemberDescriptor> {
  if (overriddenDescriptors.isEmpty()) return setOf(this)
  val roots = mutableSetOf<CallableMemberDescriptor>()
  val visited = mutableSetOf<CallableMemberDescriptor>()
  fun dfs(desc: CallableMemberDescriptor) {
    if (!visited.add(desc)) return
    if (desc.overriddenDescriptors.isEmpty()) roots.add(desc)
    else desc.overriddenDescriptors.forEach(::dfs)
  }
  dfs(this)
  return roots
}

private fun DeclarationDescriptor.isEnumSyntheticMethod(): Boolean =
  this is FunctionDescriptor &&
    (containingDeclaration as? ClassDescriptor)?.kind == ClassKind.ENUM_CLASS &&
    (name.asString() == "values" || name.asString() == "valueOf")

private fun IrDeclaration.isContainingClassKotlinOnly(): Boolean {
  val parentClass = parentClassOrNull ?: return false
  return isKotlinOnlyClass(parentClass)
}

private fun DeclarationDescriptor.isContainingClassKotlinOnly(): Boolean {
  val parentClass = containingDeclaration as? ClassDescriptor ?: return false
  return isKotlinOnlyClass(parentClass)
}

private fun IrDeclaration.overridesRealJavaMember(): Boolean {
  if (this.isDeclaredInJava() || this.isMappedJreClassMember()) return true
  if (this !is IrSimpleFunction) return false
  return overriddenSymbols.any { it.owner.overridesRealJavaMember() }
}

private fun DeclarationDescriptor.overridesRealJavaMember(): Boolean {
  if (this.isDeclaredInJava() || this.isMappedJreClassMember()) return true
  if (this !is CallableMemberDescriptor) return false
  return overriddenDescriptors.any { it.overridesRealJavaMember() }
}


