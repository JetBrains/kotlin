/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.event.DocumentAdapter
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.ui.EditorTextField
import com.siyeh.ig.psiutils.TestUtils
import org.intellij.lang.regexp.RegExpFileType
import org.jetbrains.kotlin.idea.quickfix.RenameIdentifierFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import java.awt.BorderLayout
import javax.swing.JPanel

abstract class NamingConventionInspection(private val entityName: String,
                                          defaultNamePattern: String) : AbstractKotlinInspection() {
    protected var nameRegex: Regex = defaultNamePattern.toRegex()
    var namePattern: String = defaultNamePattern
        set(value) {
            field = value
            nameRegex = value.toRegex()
        }

    protected fun verifyName(element: PsiNameIdentifierOwner, holder: ProblemsHolder) {
        val name = element.name
        val nameIdentifier = element.nameIdentifier
        if (name != null && nameIdentifier != null && !nameRegex.matches(name)) {
            holder.registerProblem(element.nameIdentifier!!,
                                   "$entityName name <code>#ref</code> doesn't match regex '$namePattern' #loc",
                                   RenameIdentifierFix())
        }
    }

    override fun createOptionsPanel() = NamingConventionOptionsPanel(this)
}

class ClassNameInspection : NamingConventionInspection("Class", "[A-Z][A-Za-z\\d]*") {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                verifyName(classOrObject, holder)
            }

            override fun visitEnumEntry(enumEntry: KtEnumEntry) {
                // do nothing
            }
        }
    }
}

class EnumEntryNameInspection : NamingConventionInspection("Enum entry", "[A-Z]([A-Za-z\\d]*|[A-Z_\\d]*)") {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitEnumEntry(enumEntry: KtEnumEntry) {
                verifyName(enumEntry, holder)
            }
        }
    }
}

class FunctionNameInspection : NamingConventionInspection("Function", "[a-z][A-Za-z\\d]*") {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                if (TestUtils.isInTestSourceContent(function) && function.nameIdentifier?.text?.startsWith("`") == true) {
                    return
                }
                verifyName(function, holder)
            }
        }
    }
}

abstract class PropertyNameInspectionBase protected constructor(private val kind: PropertyKind,
                                                                entityName: String,
                                                                defaultNamePattern: String)
    : NamingConventionInspection(entityName, defaultNamePattern) {

    protected enum class PropertyKind { NORMAL, PRIVATE, OBJECT, CONST, LOCAL }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitProperty(property: KtProperty) {
                if (property.getKind() == kind) {
                    verifyName(property, holder)
                }
            }
        }
    }

    private fun KtProperty.getKind(): PropertyKind = when {
        isLocal -> PropertyKind.LOCAL

        containingClassOrObject is KtObjectDeclaration -> PropertyKind.OBJECT

        hasModifier(KtTokens.CONST_KEYWORD) -> PropertyKind.CONST

        visibilityModifierType() == KtTokens.PRIVATE_KEYWORD -> PropertyKind.PRIVATE

        else -> PropertyKind.NORMAL
    }
}

class PropertyNameInspection : PropertyNameInspectionBase(PropertyKind.NORMAL, "Property", "[a-z][A-Za-z\\d]*")

class ObjectPropertyNameInspection : PropertyNameInspectionBase(PropertyKind.OBJECT, "Object property", "[A-Za-z][_A-Za-z\\d]*")

class PrivatePropertyNameInspection : PropertyNameInspectionBase(PropertyKind.PRIVATE, "Private property", "_?[a-z][A-Za-z\\d]*")

class ConstPropertyNameInspection : PropertyNameInspectionBase(PropertyKind.CONST, "Const property", "[A-Z][_A-Z\\d]*")

class LocalVariableNameInspection : PropertyNameInspectionBase(PropertyKind.LOCAL, "Local variable", "[a-z][A-Za-z\\d]*")

class PackageNameInspection : NamingConventionInspection("Package", "[a-z][A-Za-z\\d\\.]*") {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitPackageDirective(directive: KtPackageDirective) {
                val qualifiedName = directive.qualifiedName
                if (qualifiedName.isNotEmpty() && !nameRegex.matches(qualifiedName)) {
                    holder.registerProblem(directive.packageNameExpression!!,
                                           "Package name <code>#ref</code> doesn't match regex '$namePattern' #loc",
                                           RenamePackageFix())
                }
            }
        }
    }

    private class RenamePackageFix : RenameIdentifierFix() {
        override fun getElementToRename(element: PsiElement): PsiElement? {
            val packageDirective = element as? KtPackageDirective ?: return null
            return JavaPsiFacade.getInstance(element.project).findPackage(packageDirective.qualifiedName)
        }
    }
}

class NamingConventionOptionsPanel(owner: NamingConventionInspection) : JPanel() {
    init {
        layout = BorderLayout()

        val regexField = EditorTextField(owner.namePattern, null, RegExpFileType.INSTANCE).apply {
            setOneLineMode(true)
        }
        regexField.document.addDocumentListener(object : DocumentAdapter() {
            override fun documentChanged(e: DocumentEvent?) {
                owner.namePattern = regexField.text
            }
        })
        val labeledComponent = LabeledComponent.create(regexField, "Pattern:", BorderLayout.WEST)
        add(labeledComponent, BorderLayout.NORTH)
    }
}
