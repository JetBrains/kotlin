/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import junit.framework.TestCase
import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.descriptors.findPackageFragmentForFile
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.CHECK_COMPILE_TIME_VALUES
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.globalMetadataInfoHandler

class ConstantValuesHandler(testServices: TestServices) : ClassicFrontendAnalysisHandler(testServices) {
    companion object {
        private const val DEBUG_INFO_CONSTANT = "DEBUG_INFO_CONSTANT_VALUE"
        private val propertyNameMatchingRegex = """val ([\w\d]+)(: .*)? =""".toRegex()
    }

    enum class Mode {
        Constant,
        IsPure,
        UsesVariableAsConstant
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(DiagnosticsDirectives)

    private val metaInfoHandler = testServices.globalMetadataInfoHandler

    override fun processModule(module: TestModule, info: ClassicFrontendOutputArtifact) {
        val mode = module.directives.singleOrZeroValue(CHECK_COMPILE_TIME_VALUES) ?: return

        for ((file, ktFile) in info.ktFiles) {
            processFile(file, ktFile, info, mode)
        }
    }

    private fun processFile(file: TestFile, ktFile: KtFile, info: ClassicFrontendOutputArtifact, mode: Mode) {
        val expectedMetaInfos = metaInfoHandler.getExistingMetaInfosForFile(file).filter { it.tag == DEBUG_INFO_CONSTANT }
        val fileText = ktFile.text
        val packageFragmentDescriptor = info.analysisResult.moduleDescriptor.findPackageFragmentForFile(ktFile) ?: return
        val bindingContext = info.analysisResult.bindingContext

        val actualMetaInfos = mutableListOf<ParsedCodeMetaInfo>()
        for (expectedMetaInfo in expectedMetaInfos) {
            val start = expectedMetaInfo.start
            val end = expectedMetaInfo.end

            val markedText = fileText.substring(start, end)
            val propertyName = propertyNameMatchingRegex.find(markedText)?.groups?.get(1)?.value ?: continue
            val propertyDescriptor = getPropertyDescriptor(packageFragmentDescriptor, propertyName)
                ?: getLocalVarDescriptor(bindingContext, propertyName) ?: continue
            val actualValue = when (mode) {
                Mode.Constant -> checkConstant(propertyDescriptor)
                Mode.IsPure -> checkIsPure(bindingContext, propertyDescriptor)
                Mode.UsesVariableAsConstant -> checkVariableAsConstant(bindingContext, propertyDescriptor)
            }

            actualMetaInfos += ParsedCodeMetaInfo(
                start,
                end,
                attributes = mutableListOf(),
                tag = DEBUG_INFO_CONSTANT,
                description = actualValue
            )
        }

        metaInfoHandler.addMetadataInfosForFile(file, actualMetaInfos)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    private fun checkConstant(variableDescriptor: VariableDescriptor): String {
        val compileTimeConstant = variableDescriptor.compileTimeInitializer
        return if (compileTimeConstant is StringValue) {
            "\\\"${compileTimeConstant.value}\\\""
        } else {
            "$compileTimeConstant"
        }
    }

    private fun checkIsPure(bindingContext: BindingContext, variableDescriptor: VariableDescriptor): String {
        return evaluateInitializer(bindingContext, variableDescriptor)?.isPure.toString()
    }

    private fun checkVariableAsConstant(bindingContext: BindingContext, variableDescriptor: VariableDescriptor): String {
        return evaluateInitializer(bindingContext, variableDescriptor)?.usesVariableAsConstant.toString()
    }

    private fun evaluateInitializer(context: BindingContext, property: VariableDescriptor): CompileTimeConstant<*>? {
        val propertyDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(property) as KtProperty
        return ConstantExpressionEvaluator(property.module, LanguageVersionSettingsImpl.DEFAULT).evaluateExpression(
            propertyDeclaration.initializer!!,
            DelegatingBindingTrace(context, "trace for evaluating compile time constant"),
            property.type
        )
    }

    private fun getPropertyDescriptor(packageView: PackageFragmentDescriptor, name: String): PropertyDescriptor? {
        val propertyName = Name.identifier(name)
        val memberScope = packageView.getMemberScope()
        var properties: Collection<PropertyDescriptor?> = memberScope.getContributedVariables(propertyName, NoLookupLocation.FROM_TEST)
        if (properties.isEmpty()) {
            for (descriptor in DescriptorUtils.getAllDescriptors(memberScope)) {
                if (descriptor is ClassDescriptor) {
                    val classProperties: Collection<PropertyDescriptor?> = descriptor.getMemberScope(emptyList())
                        .getContributedVariables(propertyName, NoLookupLocation.FROM_TEST)
                    if (!classProperties.isEmpty()) {
                        properties = classProperties
                        break
                    }
                }
            }
        }
        if (properties.size != 1) {
            return null
        }
        return properties.iterator().next()
    }

    private fun getLocalVarDescriptor(context: BindingContext, name: String): VariableDescriptor? {
        for (descriptor in context.getSliceContents(BindingContext.VARIABLE).values) {
            if (descriptor.name.asString() == name) {
                return descriptor
            }
        }
        TestCase.fail("Failed to find local variable $name")
        return null
    }
}
