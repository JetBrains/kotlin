/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix.replaceJavaClass

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.search.usagesSearch.ClassUsagesSearchHelper
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearch
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchTarget
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.typeUtil.isArrayOfJavaLangClass
import org.jetbrains.kotlin.types.typeUtil.isJavaLangClass

private trait ReplacementTask {
    val element: JetElement
}
private class JavaClassCallReplacementTask(
        val javaClassCall: JetExpression,
        val className: String
) : ReplacementTask {
    override val element: JetElement = javaClassCall
}
private class JavaClassParameterReplacementTask(
    val typeReference: JetTypeReference,
    val projectionText: String
) : ReplacementTask {
    override val element: JetElement = typeReference
}

private fun renderClassNameForKClassLiteral(type: JetType): String? {
    val descriptorRenderer = IdeDescriptorRenderers.SOURCE_CODE
    if (KotlinBuiltIns.isArray(type)) {
        return descriptorRenderer.renderType(type)
    }

    val classDescriptor = (type.getConstructor().getDeclarationDescriptor() as? ClassDescriptor) ?: return null
    return descriptorRenderer.renderClassifierName(classDescriptor)
}

fun createReplacementTasks(element: JetElement, anyJavaClass: Boolean = false): List<ReplacementTask> {
    val replacementTasks = arrayListOf<ReplacementTask>()

    element.forEachDescendantsOfType(fun(expression: JetCallExpression) {
        val context = expression.analyze()
        val resolvedCall = expression.getResolvedCall(context) ?: return

        if (!anyJavaClass && !context.getDiagnostics().any { it.isJavaLangClassArgumentInAnnotation(expression) }) return

        val returnType = resolvedCall.getResultingDescriptor().getReturnType() ?: return
        if (returnType.isJavaLangClass()) {
            val inferredType = returnType.getArguments().firstOrNull()?.getType() ?: return
            if (inferredType.isError()) return
            val renderedType = renderClassNameForKClassLiteral(inferredType) ?: return
            replacementTasks.add(JavaClassCallReplacementTask(expression, renderedType))
        }
    })

    return replacementTasks
}

fun createReplacementTasksForAnnotationClass(element: JetClass): List<ReplacementTask> {
    if (!element.isAnnotation()) return emptyList()

    val replacementTasks = arrayListOf<ReplacementTask>()

    fun addJavaClassReplacementTaskByTypeReference(typeReference: JetTypeReference) {
        val classTypeArgText = typeReference.getFirstTypeArgument()?.getText() ?: return
        replacementTasks.add(JavaClassParameterReplacementTask(typeReference, classTypeArgText))
    }

    element.forEachDescendantsOfType(fun(parameter: JetParameter) {
        val valueParameterDescriptor = parameter.descriptor as? ValueParameterDescriptor ?: return
        val type = valueParameterDescriptor.getType()

        val parameterTypeReference = parameter.getTypeReference() ?: return
        if (type.isJavaLangClass() || valueParameterDescriptor.getVarargElementType()?.isJavaLangClass() ?: false) {
            addJavaClassReplacementTaskByTypeReference(parameterTypeReference)
        }
        else if (type.isArrayOfJavaLangClass()) {
            val arrayTypeArgumentReference = parameterTypeReference.getFirstTypeArgument()?.getTypeReference() ?: return
            addJavaClassReplacementTaskByTypeReference(arrayTypeArgumentReference)
        }

        val defaultValue = parameter.getDefaultValue()
        if (defaultValue != null) {
            replacementTasks.addAll(createReplacementTasks(defaultValue, anyJavaClass = true))
        }
    })

    if (replacementTasks.isEmpty()) return emptyList()

    val request = ClassUsagesSearchHelper(
            constructorUsages = true, nonConstructorUsages = false, skipImports = true
    ).newRequest(UsagesSearchTarget<JetClassOrObject>(element))

    UsagesSearch.search(request).forEach {
        ref ->
        val refElement = ref?.getElement()?.getNonStrictParentOfType<JetAnnotationEntry>()
        if (refElement != null) {
            replacementTasks.addAll(createReplacementTasks(refElement, anyJavaClass = true))
        }
    }

    return replacementTasks
}

private fun JetTypeReference.getFirstTypeArgument(): JetTypeProjection? =
        (getTypeElement() as? JetUserType)?.getTypeArguments()?.firstOrNull()

private fun Diagnostic.isJavaLangClassArgumentInAnnotation(expression: JetCallExpression) =
        getFactory() == ErrorsJvm.JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION &&
        getPsiElement().isAncestor(expression)

fun processTasks(replacementTasks: Collection<ReplacementTask>) {
    val element = replacementTasks.firstOrNull()?.element ?: return
    val psiFactory = JetPsiFactory(element)

    val elementsToShorten = arrayListOf<JetElement>()
    replacementTasks.forEach {
        task ->
        val newElement = when (task) {
            is JavaClassCallReplacementTask ->
                task.javaClassCall.replace(psiFactory.createClassLiteral(task.className)) as JetElement
            is JavaClassParameterReplacementTask ->
                task.typeReference.replace(psiFactory.createType("kotlin.reflect.KClass<${task.projectionText}>")) as JetElement
            else -> error("Unexpected task type: ${task.javaClass.getName()}")
        }

        elementsToShorten.add(newElement)
    }

    ShortenReferences.DEFAULT.process(elementsToShorten)
}
