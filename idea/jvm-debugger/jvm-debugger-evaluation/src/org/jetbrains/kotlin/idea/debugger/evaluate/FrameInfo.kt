/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.debugger.jdi.LocalVariableProxyImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.openapi.project.Project
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiNameHelper
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.safeVisibleVariables
import org.jetbrains.kotlin.idea.j2k.j2k
import org.jetbrains.kotlin.psi.KtProperty

class FrameInfo private constructor(val project: Project, thisObject: Value?, variables: Map<LocalVariableProxyImpl, Value>) {
    val thisObject = run {
        if (thisObject == null) {
            return@run null
        }

        Variable(FAKE_JAVA_THIS_NAME, thisObject.type().name(), thisObject)
    }

    val variables = variables.map { (v, value) -> Variable(v.name(), v.typeName(), value) }

    companion object {
        private const val FAKE_JAVA_THIS_NAME = "\$this\$_java_locals_debug_fun_"

        fun from(project: Project, frameProxy: StackFrameProxyImpl?): FrameInfo {
            if (frameProxy == null) {
                return FrameInfo(project, null, emptyMap())
            }

            val variableValues = collectVariableValues(frameProxy)
            return FrameInfo(project, frameProxy.thisObject(), variableValues)
        }

        private fun collectVariableValues(frameProxy: StackFrameProxyImpl): Map<LocalVariableProxyImpl, Value> {
            val variables = frameProxy.safeVisibleVariables()
            val values = HashMap<LocalVariableProxyImpl, Value>(variables.size)
            for (variable in variables) {
                val value = frameProxy.getValue(variable) ?: continue
                values[variable] = value
            }
            return values
        }

        private fun createKotlinProperty(project: Project, name: String, typeName: String, value: Value?): KtProperty? {
            val className = typeName.replace("$", ".").substringBefore("[]")
            val classType = PsiType.getTypeByName(className, project, GlobalSearchScope.allScope(project))

            val elementType = when {
                value !is PrimitiveValue && classType.resolve() == null -> CommonClassNames.JAVA_LANG_OBJECT
                else -> className
            }

            val propertyType = if (value is ArrayReference) "$elementType[]" else elementType
            val psiType = PsiType.getTypeByName(propertyType, project, GlobalSearchScope.allScope(project))

            val field = PsiElementFactory.SERVICE.getInstance(project).createField(name, psiType)
            val ktProperty = field.j2k() as? KtProperty
            ktProperty?.modifierList?.delete()
            return ktProperty
        }
    }

    inner class Variable(val name: String, val typeName: String, val value: Value?) {
        fun asProperty(): KtProperty? {
            if (!PsiNameHelper.getInstance(project).isIdentifier(name)) {
                return null
            }

            return createKotlinProperty(project, name, typeName, value)
        }
    }
}