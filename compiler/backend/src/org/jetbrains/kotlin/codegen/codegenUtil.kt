/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.jetbrains.kotlin.codegen

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext
import org.jetbrains.kotlin.codegen.context.PackageContext
import org.jetbrains.kotlin.codegen.intrinsics.TypeIntrinsics
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.SpecialSignatureInfo
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.serialization.deserialization.PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method
import java.util.*

fun generateIsCheck(
        v: InstructionAdapter,
        kotlinType: KotlinType,
        asmType: Type
) {
    if (TypeUtils.isNullableType(kotlinType)) {
        val nope = Label()
        val end = Label()

        with(v) {
            dup()

            ifnull(nope)

            TypeIntrinsics.instanceOf(this, kotlinType, asmType)

            goTo(end)

            mark(nope)
            pop()
            iconst(1)

            mark(end)
        }
    }
    else {
        TypeIntrinsics.instanceOf(v, kotlinType, asmType)
    }
}

fun generateAsCast(
        v: InstructionAdapter,
        kotlinType: KotlinType,
        asmType: Type,
        isSafe: Boolean
) {
    if (!isSafe) {
        if (!TypeUtils.isNullableType(kotlinType)) {
            generateNullCheckForNonSafeAs(v, kotlinType)
        }
    }
    else {
        with(v) {
            dup()
            TypeIntrinsics.instanceOf(v, kotlinType, asmType)
            val ok = Label()
            ifne(ok)
            pop()
            aconst(null)
            mark(ok)
        }
    }

    TypeIntrinsics.checkcast(v, kotlinType, asmType, isSafe)
}

private fun generateNullCheckForNonSafeAs(
        v: InstructionAdapter,
        type: KotlinType
) {
    with(v) {
        dup()
        val nonnull = Label()
        ifnonnull(nonnull)
        AsmUtil.genThrow(v, "kotlin/TypeCastException", "null cannot be cast to non-null type " + DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type))
        mark(nonnull)
    }
}

fun SpecialSignatureInfo.replaceValueParametersIn(sourceSignature: String?): String?
        = valueParametersSignature?.let { sourceSignature?.replace("^\\(.*\\)".toRegex(), "($it)") }

fun populateCompanionBackingFieldNamesToOuterContextIfNeeded(companion: KtObjectDeclaration, outerContext: FieldOwnerContext<*>, state: GenerationState) {
    val descriptor = state.bindingContext.get(BindingContext.CLASS, companion)

    if (descriptor == null || ErrorUtils.isError(descriptor)) {
        return
    }

    if (!JvmAbi.isCompanionObjectWithBackingFieldsInOuter(descriptor)) {
        return
    }
    val properties = companion.declarations.filterIsInstance<KtProperty>()

    properties.forEach {
        val variableDescriptor = state.bindingContext.get(BindingContext.VARIABLE, it)
        if (variableDescriptor is PropertyDescriptor) {
            outerContext.getFieldName(variableDescriptor, it.hasDelegate())
        }
    }

}

// TODO: inline and remove then ScriptCodegen is converted to Kotlin
fun mapSupertypesNames(typeMapper: KotlinTypeMapper, supertypes: List<ClassDescriptor>, signatureVisitor: JvmSignatureWriter?): Array<String> =
        supertypes.map { typeMapper.mapSupertype(it.defaultType, signatureVisitor).internalName }.toTypedArray()


// Top level subclasses of a sealed class should be generated before that sealed class,
// so that we'd generate the necessary accessor for its constructor afterwards
fun sortTopLevelClassesAndPrepareContextForSealedClasses(
        classOrObjects: List<KtClassOrObject>,
        packagePartContext: PackageContext,
        state: GenerationState
): List<KtClassOrObject> {
    fun prepareContextIfNeeded(descriptor: ClassDescriptor?) {
        if (DescriptorUtils.isSealedClass(descriptor)) {
            // save context for sealed class
            packagePartContext.intoClass(descriptor!!, OwnerKind.IMPLEMENTATION, state)
        }
    }

    // optimization
    when (classOrObjects.size) {
        0 -> return emptyList()
        1 -> {
            prepareContextIfNeeded(state.bindingContext.get(BindingContext.CLASS, classOrObjects.first()))
            return classOrObjects
        }
    }

    val result = ArrayList<KtClassOrObject>(classOrObjects.size)
    val descriptorToPsi = LinkedHashMap<ClassDescriptor, KtClassOrObject>()
    for (classOrObject in classOrObjects) {
        val descriptor = state.bindingContext.get(BindingContext.CLASS, classOrObject)
        if (descriptor == null) {
            result.add(classOrObject)
        }
        else {
            prepareContextIfNeeded(descriptor)
            descriptorToPsi[descriptor] = classOrObject
        }
    }

    // topologicalOrder(listOf(1, 2, 3)) { emptyList() } = listOf(3, 2, 1). Because of this used keys.reversed().
    val sortedDescriptors = DFS.topologicalOrder(descriptorToPsi.keys.reversed()) {
        it.typeConstructor.supertypes.map { it.constructor.declarationDescriptor as? ClassDescriptor }.filter { it in descriptorToPsi.keys }
    }
    sortedDescriptors.mapTo(result) { descriptorToPsi[it]!! }
    return result
}

fun CallableMemberDescriptor.isDefinitelyNotDefaultImplsMethod() =
        this is JavaCallableMemberDescriptor || this.annotations.hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME)


fun ClassBuilder.generateMethod(
        debugString: String,
        access: Int,
        method: Method,
        element: PsiElement?,
        origin: JvmDeclarationOrigin,
        state: GenerationState,
        generate: InstructionAdapter.() -> Unit
) {
    val mv = this.newMethod(origin, access, method.name, method.descriptor, null, null)

    if (state.classBuilderMode.generateBodies) {
        val iv = InstructionAdapter(mv)
        iv.visitCode()
        iv.generate()
        iv.areturn(method.returnType)
        FunctionCodegen.endVisit(mv, debugString, element)
    }
}


fun reportTarget6InheritanceErrorIfNeeded(
        classDescriptor: ClassDescriptor, classElement: KtClassOrObject, restrictedInheritance: List<FunctionDescriptor>, state:GenerationState
) {
    if (!restrictedInheritance.isEmpty()) {
        val groupBy = restrictedInheritance.groupBy { descriptor -> descriptor.containingDeclaration as ClassDescriptor }

        for ((key, value) in groupBy) {
            state.diagnostics.report(
                    ErrorsJvm.TARGET6_INTERFACE_INHERITANCE.on(
                            classElement, classDescriptor, key,
                            value.map { Renderers.COMPACT.render(JvmCodegenUtil.getDirectMember(it), RenderingContext.Empty) }.
                                    joinToString(separator = "\n", prefix = "\n")
                    )
            )
        }
    }
}

fun CallableDescriptor.isJvmStaticInObjectOrClass(): Boolean =
        isJvmStaticIn {
            DescriptorUtils.isNonCompanionObject(it) ||
            // This is necessary because for generation of @JvmStatic methods from companion of class A
            // we create a synthesized descriptor containing in class A
            DescriptorUtils.isClassOrEnumClass(it)
        }

fun CallableDescriptor.isJvmStaticInCompanionObject(): Boolean =
        isJvmStaticIn { DescriptorUtils.isCompanionObject(it) }

private fun CallableDescriptor.isJvmStaticIn(predicate: (DeclarationDescriptor) -> Boolean): Boolean =
        when (this) {
            is PropertyAccessorDescriptor -> {
                val propertyDescriptor = correspondingProperty
                predicate(propertyDescriptor.containingDeclaration) &&
                (hasJvmStaticAnnotation() || propertyDescriptor.hasJvmStaticAnnotation())
            }
            else -> predicate(containingDeclaration) && hasJvmStaticAnnotation()
        }
