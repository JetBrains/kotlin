/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
import java.util.regex.PatternSyntaxException
import javax.swing.JPanel

data class NamingRule(val message: String, val matcher: (String) -> Boolean)

private val START_UPPER = NamingRule("should start with an uppercase letter") {
    it.getOrNull(0)?.isUpperCase() == false
}

private val START_LOWER = NamingRule("should start with a lowercase letter") {
    it.getOrNull(0)?.isLowerCase() == false
}

private val NO_UNDERSCORES = NamingRule("should not contain underscores") {
    '_' in it
}

private val NO_START_UNDERSCORE = NamingRule("should not start with an underscore") {
    it.startsWith('_')
}

private val NO_MIDDLE_UNDERSCORES = NamingRule("should not contain underscores in the middle or the end") {
    '_' in it.substring(1)
}

abstract class NamingConventionInspection(
    private val entityName: String,
    private val defaultNamePattern: String,
    private vararg val rules: NamingRule
) : AbstractKotlinInspection() {
    protected var nameRegex: Regex? = defaultNamePattern.toRegex()
    var namePattern: String = defaultNamePattern
        set(value) {
            field = value
            nameRegex = try {
                value.toRegex()
            } catch (e: PatternSyntaxException) {
                null
            }
        }

    protected fun verifyName(element: PsiNameIdentifierOwner, holder: ProblemsHolder) {
        val name = element.name
        val nameIdentifier = element.nameIdentifier
        if (name != null && nameIdentifier != null && nameRegex?.matches(name) == false) {
            val message = getNameMismatchMessage(name)
            holder.registerProblem(
                element.nameIdentifier!!,
                "$entityName name <code>#ref</code> $message #loc",
                RenameIdentifierFix()
            )
        }
    }

    protected fun getNameMismatchMessage(name: String): String {
        if (namePattern == defaultNamePattern) {
            for (rule in rules) {
                if (rule.matcher(name)) {
                    return rule.message
                }
            }
        }
        return "doesn't match regex '$namePattern'"
    }

    override fun createOptionsPanel() = NamingConventionOptionsPanel(this)
}

class ClassNameInspection : NamingConventionInspection(
    "Class",
    "[A-Z][A-Za-z\\d]*",
    START_UPPER, NO_UNDERSCORES
) {
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

class EnumEntryNameInspection : NamingConventionInspection(
    "Enum entry",
    "[A-Z]([A-Za-z\\d]*|[A-Z_\\d]*)",
    START_UPPER
) {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitEnumEntry(enumEntry: KtEnumEntry) {
                verifyName(enumEntry, holder)
            }
        }
    }
}

class FunctionNameInspection : NamingConventionInspection(
    "Function",
    "[a-z][A-Za-z\\d]*",
    START_LOWER, NO_UNDERSCORES
) {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                if (TestUtils.isInTestSourceContent(function)) {
                    return
                }
                verifyName(function, holder)
            }
        }
    }
}

class TestFunctionNameInspection : NamingConventionInspection(
    "Test function",
    "[a-z][A-Za-z_\\d]*",
    START_LOWER
) {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                if (!TestUtils.isInTestSourceContent(function)) {
                    return
                }
                if (function.nameIdentifier?.text?.startsWith("`") == true) {
                    return
                }
                verifyName(function, holder)
            }
        }
    }
}

abstract class PropertyNameInspectionBase protected constructor(
    private val kind: PropertyKind,
    entityName: String,
    defaultNamePattern: String,
    vararg rules: NamingRule
) : NamingConventionInspection(entityName, defaultNamePattern, *rules) {

    protected enum class PropertyKind { NORMAL, PRIVATE, OBJECT_OR_TOP_LEVEL, CONST, LOCAL }

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

        containingClassOrObject is KtObjectDeclaration -> PropertyKind.OBJECT_OR_TOP_LEVEL

        isTopLevel -> PropertyKind.OBJECT_OR_TOP_LEVEL

        hasModifier(KtTokens.CONST_KEYWORD) -> PropertyKind.CONST

        visibilityModifierType() == KtTokens.PRIVATE_KEYWORD -> PropertyKind.PRIVATE

        else -> PropertyKind.NORMAL
    }
}

class PropertyNameInspection :
    PropertyNameInspectionBase(PropertyKind.NORMAL, "Property", "[a-z][A-Za-z\\d]*", START_LOWER, NO_UNDERSCORES)

class ObjectPropertyNameInspection :
    PropertyNameInspectionBase(
        PropertyKind.OBJECT_OR_TOP_LEVEL,
        "Object or top-level property",
        "[A-Za-z][_A-Za-z\\d]*",
        NO_START_UNDERSCORE
    )

class PrivatePropertyNameInspection :
    PropertyNameInspectionBase(PropertyKind.PRIVATE, "Private property", "_?[a-z][A-Za-z\\d]*", NO_MIDDLE_UNDERSCORES)

class ConstPropertyNameInspection :
    PropertyNameInspectionBase(PropertyKind.CONST, "Const property", "[A-Z][_A-Z\\d]*")

class LocalVariableNameInspection :
    PropertyNameInspectionBase(PropertyKind.LOCAL, "Local variable", "[a-z][A-Za-z\\d]*", START_LOWER, NO_UNDERSCORES)

class PackageNameInspection :
    NamingConventionInspection("Package", "[a-z][A-Za-z\\d]*(\\.[a-z][A-Za-z\\d]*)*", NO_UNDERSCORES) {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitPackageDirective(directive: KtPackageDirective) {
                val qualifiedName = directive.qualifiedName
                if (qualifiedName.isNotEmpty() && nameRegex?.matches(qualifiedName) == false) {
                    val message = getNameMismatchMessage(qualifiedName)
                    holder.registerProblem(
                        directive.packageNameExpression!!,
                        "Package name <code>#ref</code> $message #loc",
                        RenamePackageFix()
                    )
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
