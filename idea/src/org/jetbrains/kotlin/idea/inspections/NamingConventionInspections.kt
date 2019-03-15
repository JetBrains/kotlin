/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.*
import com.intellij.codeInspection.reference.RefEntity
import com.intellij.codeInspection.reference.RefFile
import com.intellij.codeInspection.reference.RefPackage
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.ui.EditorTextField
import com.siyeh.ig.BaseGlobalInspection
import com.siyeh.ig.psiutils.TestUtils
import org.intellij.lang.annotations.Language
import org.intellij.lang.regexp.RegExpFileType
import org.jdom.Element
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.core.packageMatchesDirectoryOrImplicit
import org.jetbrains.kotlin.idea.quickfix.RenameIdentifierFix
import org.jetbrains.kotlin.idea.refactoring.isInjectedFragment
import org.jetbrains.kotlin.idea.util.compat.psiFile
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import java.awt.BorderLayout
import java.util.regex.PatternSyntaxException
import javax.swing.JPanel

data class NamingRule(val message: String, val matcher: (String) -> Boolean)

private fun findRuleMessage(checkString: String, rules: Array<out NamingRule>): String? {
    for (rule in rules) {
        if (rule.matcher(checkString)) {
            return rule.message
        }
    }

    return null
}

private val START_UPPER = NamingRule("should start with an uppercase letter") {
    it.getOrNull(0)?.isUpperCase() == false
}

private val START_LOWER = NamingRule("should start with a lowercase letter") {
    it.getOrNull(0)?.isLowerCase() == false
}

private val NO_UNDERSCORES = NamingRule("should not contain underscores") {
    '_' in it
}

private val NO_START_UPPER = NamingRule("should not start with an uppercase letter") {
    it.getOrNull(0)?.isUpperCase() == true
}

private val NO_START_UNDERSCORE = NamingRule("should not start with an underscore") {
    it.startsWith('_')
}

private val NO_MIDDLE_UNDERSCORES = NamingRule("should not contain underscores in the middle or the end") {
    '_' in it.substring(1)
}

private val NO_BAD_CHARACTERS = NamingRule("may contain only letters and digits") {
    it.any { c -> c !in 'a'..'z' && c !in 'A'..'Z' && c !in '0'..'9' }
}

private val NO_BAD_CHARACTERS_OR_UNDERSCORE = NamingRule("may contain only letters, digits or underscores") {
    it.any { c -> c !in 'a'..'z' && c !in 'A'..'Z' && c !in '0'..'9' && c != '_' }
}

class NamingConventionInspectionSettings(
    private val entityName: String,
    @Language("RegExp") val defaultNamePattern: String,
    private val setNamePatternCallback: ((value: String) -> Unit),
    private vararg val rules: NamingRule
) {
    var nameRegex: Regex? = defaultNamePattern.toRegex()

    var namePattern: String = defaultNamePattern
        set(value) {
            field = value
            setNamePatternCallback.invoke(value)
            nameRegex = try {
                value.toRegex()
            } catch (e: PatternSyntaxException) {
                null
            }
        }

    fun verifyName(element: PsiNameIdentifierOwner, holder: ProblemsHolder) {
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

    fun getNameMismatchMessage(name: String): String {
        if (namePattern != defaultNamePattern) {
            return getDefaultErrorMessage()
        }

        return findRuleMessage(name, rules) ?: getDefaultErrorMessage()
    }

    fun getDefaultErrorMessage() = "doesn't match regex '$namePattern'"

    fun createOptionsPanel(): JPanel = NamingConventionOptionsPanel(this)

    private class NamingConventionOptionsPanel(settings: NamingConventionInspectionSettings) : JPanel() {
        init {
            layout = BorderLayout()

            val regexField = EditorTextField(settings.namePattern, null, RegExpFileType.INSTANCE).apply {
                setOneLineMode(true)
            }
            regexField.document.addDocumentListener(object : DocumentListener {
                override fun documentChanged(e: DocumentEvent) {
                    settings.namePattern = regexField.text
                }
            })
            val labeledComponent = LabeledComponent.create(regexField, "Pattern:", BorderLayout.WEST)
            add(labeledComponent, BorderLayout.NORTH)
        }
    }
}


sealed class NamingConventionInspection(
    entityName: String,
    @Language("RegExp") defaultNamePattern: String,
    vararg rules: NamingRule
) : AbstractKotlinInspection() {

    // Serialized inspection state
    @Suppress("MemberVisibilityCanBePrivate")
    var namePattern: String = defaultNamePattern

    private val namingSettings = NamingConventionInspectionSettings(
        entityName, defaultNamePattern,
        setNamePatternCallback = { value ->
            namePattern = value
        },
        rules = *rules
    )

    protected fun verifyName(element: PsiNameIdentifierOwner, holder: ProblemsHolder) {
        namingSettings.verifyName(element, holder)
    }

    protected fun getNameMismatchMessage(name: String): String {
        return namingSettings.getNameMismatchMessage(name)
    }

    override fun createOptionsPanel(): JPanel = namingSettings.createOptionsPanel()

    override fun readSettings(node: Element) {
        super.readSettings(node)
        namingSettings.namePattern = namePattern
    }
}

class ClassNameInspection : NamingConventionInspection(
    "Class",
    "[A-Z][A-Za-z\\d]*",
    START_UPPER, NO_UNDERSCORES, NO_BAD_CHARACTERS
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
    START_UPPER, NO_BAD_CHARACTERS_OR_UNDERSCORE
) {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return enumEntryVisitor { enumEntry -> verifyName(enumEntry, holder) }
    }
}

class FunctionNameInspection : NamingConventionInspection(
    "Function",
    "[a-z][A-Za-z\\d]*",
    START_LOWER, NO_UNDERSCORES, NO_BAD_CHARACTERS
) {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return namedFunctionVisitor { function ->
            if (function.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
                return@namedFunctionVisitor
            }
            if (!TestUtils.isInTestSourceContent(function)) {
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
        return namedFunctionVisitor { function ->
            if (!TestUtils.isInTestSourceContent(function)) {
                return@namedFunctionVisitor
            }
            if (function.nameIdentifier?.text?.startsWith("`") == true) {
                return@namedFunctionVisitor
            }
            verifyName(function, holder)
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
        return propertyVisitor { property ->
            if (property.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
                return@propertyVisitor
            }

            if (property.getKind() == kind) {
                verifyName(property, holder)
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
    PropertyNameInspectionBase(
        PropertyKind.NORMAL, "Property", "[a-z][A-Za-z\\d]*",
        START_LOWER, NO_UNDERSCORES, NO_BAD_CHARACTERS
    )

class ObjectPropertyNameInspection :
    PropertyNameInspectionBase(
        PropertyKind.OBJECT_OR_TOP_LEVEL,
        "Object or top-level property",
        "[A-Za-z][_A-Za-z\\d]*",
        NO_START_UNDERSCORE, NO_BAD_CHARACTERS_OR_UNDERSCORE
    )

class PrivatePropertyNameInspection :
    PropertyNameInspectionBase(
        PropertyKind.PRIVATE, "Private property", "_?[a-z][A-Za-z\\d]*",
        NO_MIDDLE_UNDERSCORES, NO_BAD_CHARACTERS_OR_UNDERSCORE
    )

class ConstPropertyNameInspection :
    PropertyNameInspectionBase(PropertyKind.CONST, "Const property", "[A-Z][_A-Z\\d]*")

class LocalVariableNameInspection :
    PropertyNameInspectionBase(
        PropertyKind.LOCAL, "Local variable", "[a-z][A-Za-z\\d]*",
        START_LOWER, NO_UNDERSCORES, NO_BAD_CHARACTERS
    )

private class PackageNameInspectionLocal(
    val parentInspection: InspectionProfileEntry,
    val namingSettings: NamingConventionInspectionSettings
) : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return packageDirectiveVisitor { directive ->
            val packageNameExpression = directive.packageNameExpression ?: return@packageDirectiveVisitor

            val checkResult = checkPackageDirective(directive, namingSettings) ?: return@packageDirectiveVisitor

            val descriptionTemplate = checkResult.toProblemTemplateString()

            holder.registerProblem(
                packageNameExpression,
                descriptionTemplate,
                RenamePackageFix()
            )
        }
    }

    companion object {
        data class CheckResult(val errorMessage: String, val isForPart: Boolean)

        fun CheckResult.toProblemTemplateString(): String {
            return if (isForPart) {
                "Package name <code>#ref</code> part $errorMessage #loc"
            } else {
                "Package name <code>#ref</code> $errorMessage #loc"
            }
        }

        fun checkPackageDirective(directive: KtPackageDirective, namingSettings: NamingConventionInspectionSettings): CheckResult? {
            return checkQualifiedName(directive.qualifiedName, namingSettings)
        }

        fun checkQualifiedName(qualifiedName: String, namingSettings: NamingConventionInspectionSettings): CheckResult? {
            if (qualifiedName.isEmpty() || namingSettings.nameRegex?.matches(qualifiedName) != false) {
                return null
            }

            val partErrorMessage = if (namingSettings.namePattern == namingSettings.defaultNamePattern) {
                qualifiedName.split('.').asSequence()
                    .mapNotNull { part -> findRuleMessage(part, PackageNameInspection.PART_RULES) }
                    .firstOrNull()
            } else {
                null
            }

            return if (partErrorMessage != null) {
                CheckResult(partErrorMessage, true)
            } else {
                CheckResult(namingSettings.getDefaultErrorMessage(), false)
            }
        }
    }

    private class RenamePackageFix : RenameIdentifierFix() {
        override fun getElementToRename(element: PsiElement): PsiElement? {
            val packageDirective = element as? KtPackageDirective ?: return null
            return JavaPsiFacade.getInstance(element.project).findPackage(packageDirective.qualifiedName)
        }
    }

    override fun getShortName(): String = parentInspection.shortName
    override fun getDisplayName(): String = parentInspection.displayName
}

class PackageNameInspection : BaseGlobalInspection() {
    companion object {
        const val DEFAULT_PACKAGE_NAME_PATTERN = "[a-z_][a-zA-Z\\d_]*(\\.[a-z_][a-zA-Z\\d_]*)*"
        val PART_RULES = arrayOf(NO_BAD_CHARACTERS_OR_UNDERSCORE, NO_START_UPPER)

        private fun PackageNameInspectionLocal.Companion.CheckResult.toErrorMessage(qualifiedName: String): String {
            return if (isForPart) {
                "Package name <code>$qualifiedName</code> part $errorMessage"
            } else {
                "Package name <code>$qualifiedName</code> $errorMessage"
            }
        }
    }

    // Serialized setting
    @Suppress("MemberVisibilityCanBePrivate")
    var namePattern: String = DEFAULT_PACKAGE_NAME_PATTERN

    private val namingSettings = NamingConventionInspectionSettings(
        "Package",
        DEFAULT_PACKAGE_NAME_PATTERN,
        setNamePatternCallback = { value ->
            namePattern = value
        }
    )

    override fun checkElement(
        refEntity: RefEntity,
        analysisScope: AnalysisScope,
        inspectionManager: InspectionManager,
        globalInspectionContext: GlobalInspectionContext
    ): Array<CommonProblemDescriptor>? {
        when (refEntity) {
            is RefFile -> {
                val psiFile = refEntity.psiFile
                if (psiFile is KtFile && !psiFile.isInjectedFragment && !psiFile.packageMatchesDirectoryOrImplicit()) {
                    val packageDirective = psiFile.packageDirective
                    if (packageDirective != null) {
                        val qualifiedName = packageDirective.qualifiedName
                        val checkResult = PackageNameInspectionLocal.checkPackageDirective(packageDirective, namingSettings)
                        if (checkResult != null) {
                            return arrayOf(inspectionManager.createProblemDescriptor(checkResult.toErrorMessage(qualifiedName)))
                        }
                    }
                }
            }

            is RefPackage -> {
                @NonNls val name = StringUtil.getShortName(refEntity.getQualifiedName())
                if (name.isEmpty() || InspectionsBundle.message("inspection.reference.default.package") == name) {
                    return null
                }

                val checkResult = PackageNameInspectionLocal.checkQualifiedName(name, namingSettings)
                if (checkResult != null) {
                    return arrayOf(inspectionManager.createProblemDescriptor(checkResult.toErrorMessage(name)))
                }
            }

            else -> {
                return null
            }
        }

        return null
    }

    override fun readSettings(element: Element) {
        super.readSettings(element)
        namingSettings.namePattern = namePattern
    }

    override fun createOptionsPanel() = namingSettings.createOptionsPanel()

    override fun getSharedLocalInspectionTool(): LocalInspectionTool {
        return PackageNameInspectionLocal(this, namingSettings)
    }
}