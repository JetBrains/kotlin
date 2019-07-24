/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.openapi.project.Project
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiNameHelper
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.j2k.j2k
import org.jetbrains.kotlin.psi.KtProperty

class FrameInfo private constructor(val project: Project, thisObject: Value?, variables: Map<LocalVariable, Value?>) {
    val thisObject = run {
        if (thisObject == null) {
            return@run null
        }

        Variable(FAKE_JAVA_THIS_NAME, thisObject.type().name(), thisObject)
    }

    val variables = variables.map { (v, value) -> Variable(v.name(), v.typeName(), value) }

    companion object {
        private const val FAKE_JAVA_THIS_NAME = "\$this\$_java_locals_debug_fun_"

        fun from(project: Project, frame: StackFrame?): FrameInfo {
            if (frame == null) {
                return FrameInfo(project, null, emptyMap())
            }

            return FrameInfo(project, frame.thisObject(), frame.getValues(frame.visibleVariables()))
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