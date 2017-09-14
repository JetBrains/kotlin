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

package org.jetbrains.kotlin.idea.quickfix.createImpl

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ide.util.PackageUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.refactoring.createKotlinFile
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.resolve.getMultiTargetPlatform

sealed class CreateActualFix<out D : KtNamedDeclaration>(
        declaration: D,
        private val actualPlatform: MultiTargetPlatform.Specific,
        private val generateIt: KtPsiFactory.(Project, D) -> D?
) : KotlinQuickFixAction<D>(declaration) {

    override fun getFamilyName() = text

    protected abstract val elementType: String

    override fun getText() = "Create actual $elementType for platform ${actualPlatform.platform}"

    override fun startInWriteAction() = false

    override final fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val factory = KtPsiFactory(project)

        val actualFile = getOrCreateImplementationFile(project) ?: return
        val generated = factory.generateIt(project, element) ?: return

        runWriteAction {
            if (actualFile.packageDirective?.fqName != file.packageDirective?.fqName &&
                actualFile.declarations.isEmpty()) {
                val packageDirective = file.packageDirective
                packageDirective?.let {
                    val oldPackageDirective = actualFile.packageDirective
                    val newPackageDirective = factory.createPackageDirective(it.fqName)
                    if (oldPackageDirective != null) {
                        oldPackageDirective.replace(newPackageDirective)
                    }
                    else {
                        actualFile.add(newPackageDirective)
                    }
                }
            }
            val actualDeclaration = actualFile.add(generated) as KtElement
            val reformatted = CodeStyleManager.getInstance(project).reformat(actualDeclaration)
            ShortenReferences.DEFAULT.process(reformatted as KtElement)
        }
    }

    private fun Project.implementationModuleOf(expectedModule: Module) =
            allModules().firstOrNull {
                PackageUtil.checkSourceRootsConfigured(it, false) &&
                TargetPlatformDetector.getPlatform(it).multiTargetPlatform == actualPlatform &&
                expectedModule in ModuleRootManager.getInstance(it).dependencies
            }

    private fun getOrCreateImplementationFile(
            project: Project
    ): KtFile? {
        val declaration = element as? KtNamedDeclaration ?: return null
        val name = declaration.name ?: return null

        val expectedDir = declaration.containingFile.containingDirectory
        val expectedPackage = JavaDirectoryService.getInstance().getPackage(expectedDir)

        val expectedModule = ModuleUtilCore.findModuleForPsiElement(declaration) ?: return null
        val actualModule = project.implementationModuleOf(expectedModule) ?: return null
        val actualDirectory = PackageUtil.findOrCreateDirectoryForPackage(
                actualModule, expectedPackage?.qualifiedName ?: "", null, false
        ) ?: return null
        return runWriteAction {
            val fileName = "$name.kt"
            val existingFile = actualDirectory.findFile(fileName)
            val packageDirective = declaration.containingKtFile.packageDirective
            val packageName =
                    if (packageDirective?.packageNameExpression == null) actualDirectory.getPackage()?.qualifiedName
                    else packageDirective.fqName.asString()
            if (existingFile is KtFile) {
                val existingPackageDirective = existingFile.packageDirective
                if (existingFile.declarations.isNotEmpty() &&
                    existingPackageDirective?.fqName != packageDirective?.fqName) {
                    val newName = KotlinNameSuggester.suggestNameByName(name) {
                        actualDirectory.findFile("$it.kt") == null
                    } + ".kt"
                    createKotlinFile(newName, actualDirectory, packageName)
                }
                else {
                    existingFile
                }
            }
            else {
                createKotlinFile(fileName, actualDirectory, packageName)
            }
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val d = DiagnosticFactory.cast(diagnostic, Errors.HEADER_WITHOUT_IMPLEMENTATION)
            val declaration = d.psiElement as? KtNamedDeclaration ?: return null
            val compatibility = d.c
            if (compatibility.isNotEmpty()) return null
            val actualPlatform = d.b.getMultiTargetPlatform() as? MultiTargetPlatform.Specific ?: return null
            return when (declaration) {
                is KtClassOrObject -> CreateActualClassFix(declaration, actualPlatform)
                is KtFunction -> CreateActualFunctionFix(declaration, actualPlatform)
                is KtProperty -> CreateActualPropertyFix(declaration, actualPlatform)
                else -> null
            }
        }
    }
}

class CreateActualClassFix(
        klass: KtClassOrObject,
        actualPlatform: MultiTargetPlatform.Specific
) : CreateActualFix<KtClassOrObject>(klass, actualPlatform, { project, element ->
    generateClassOrObject(project, element, actualNeeded = true)
}) {

    override val elementType = if ((element as? KtClass)?.isInterface() == true) "interface" else "class"
}

class CreateActualPropertyFix(
        property: KtProperty,
        actualPlatform: MultiTargetPlatform.Specific
) : CreateActualFix<KtProperty>(property, actualPlatform, { project, element ->
    val descriptor = element.toDescriptor() as? PropertyDescriptor
    descriptor?.let { generateProperty(project, element, descriptor, actualNeeded = true) }
}) {

    override val elementType = "property"
}

class CreateActualFunctionFix(
        function: KtFunction,
        actualPlatform: MultiTargetPlatform.Specific
) : CreateActualFix<KtFunction>(function, actualPlatform, { project, element ->
    val descriptor = element.toDescriptor() as? FunctionDescriptor
    descriptor?.let { generateFunction(project, element, descriptor, actualNeeded = true) }
}) {

    override val elementType = "function"
}

private fun KtModifierListOwner.replaceExpectModifier(actualNeeded: Boolean) {
    if (actualNeeded) {
        addModifier(KtTokens.ACTUAL_KEYWORD)
    }
    else {
        removeModifier(KtTokens.HEADER_KEYWORD)
        removeModifier(KtTokens.EXPECT_KEYWORD)
    }
}

private fun KtPsiFactory.generateClassOrObject(
        project: Project,
        expectedClass: KtClassOrObject,
        actualNeeded: Boolean
): KtClassOrObject {
    val expectedText = expectedClass.text
    val actualClass = if (expectedClass is KtObjectDeclaration) createObject(expectedText) else createClass(expectedText)
    if (expectedClass !is KtClass || !expectedClass.isInterface()) {
        actualClass.declarations.forEach {
            if (it !is KtEnumEntry &&
                it !is KtClassOrObject &&
                !it.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
                it.delete()
            }
        }

        declLoop@ for (expectedDeclaration in expectedClass.declarations) {
            if (expectedDeclaration.hasModifier(KtTokens.ABSTRACT_KEYWORD)) continue
            val descriptor = expectedDeclaration.toDescriptor() ?: continue
            val actualDeclaration: KtDeclaration = when (expectedDeclaration) {
                is KtFunction -> generateFunction(project, expectedDeclaration, descriptor as FunctionDescriptor, actualNeeded = true)
                is KtProperty -> generateProperty(project, expectedDeclaration, descriptor as PropertyDescriptor, actualNeeded = true)
                else -> continue@declLoop
            }
            actualClass.addDeclaration(actualDeclaration)
        }
    }

    return actualClass.apply {
        replaceExpectModifier(actualNeeded)
    }
}

private fun KtPsiFactory.generateFunction(
        project: Project,
        expectedFunction: KtFunction,
        descriptor: FunctionDescriptor,
        actualNeeded: Boolean
): KtFunction {
    val returnType = descriptor.returnType
    val body = run {
        if (returnType != null && !KotlinBuiltIns.isUnit(returnType)) {
            val delegation = getFunctionBodyTextFromTemplate(
                    project,
                    TemplateKind.FUNCTION,
                    descriptor.name.asString(),
                    IdeDescriptorRenderers.SOURCE_CODE.renderType(returnType)
            )

            "{$delegation\n}"
        }
        else {
            "{}"
        }
    }

    return if (expectedFunction is KtSecondaryConstructor) {
        createSecondaryConstructor(expectedFunction.text + " " + body)
    }
    else {
        createFunction(expectedFunction.text + " " + body).apply {
            replaceExpectModifier(actualNeeded)
            if (returnType != null && KotlinBuiltIns.isUnit(returnType)) {
                typeReference = null
            }
        }
    }
}

private fun KtPsiFactory.generateProperty(
        project: Project,
        expectedProperty: KtProperty,
        descriptor: PropertyDescriptor,
        actualNeeded: Boolean
): KtProperty {
    val body = buildString {
        append("\nget()")
        append(" = ")
        append(getFunctionBodyTextFromTemplate(
                project,
                TemplateKind.FUNCTION,
                descriptor.name.asString(),
                descriptor.returnType?.let { IdeDescriptorRenderers.SOURCE_CODE.renderType(it) } ?: ""
        ))
        if (descriptor.isVar) {
            append("\nset(value) {}")
        }
    }
    return createProperty(expectedProperty.text + " " + body).apply {
        replaceExpectModifier(actualNeeded)
    }
}

