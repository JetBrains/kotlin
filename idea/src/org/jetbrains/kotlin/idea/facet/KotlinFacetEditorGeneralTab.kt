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

package org.jetbrains.kotlin.idea.facet

import com.intellij.facet.impl.ui.libraries.DelegatingLibrariesValidatorContext
import com.intellij.facet.ui.*
import com.intellij.facet.ui.libraries.FrameworkLibraryValidator
import com.intellij.ide.actions.ShowSettingsUtilImpl
import com.intellij.openapi.project.Project
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.HoverHyperlinkLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.ThreeStateCheckBox
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.config.createCompilerArguments
import org.jetbrains.kotlin.config.splitArgumentString
import org.jetbrains.kotlin.idea.compiler.configuration.*
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import kotlin.reflect.full.findAnnotation

class KotlinFacetEditorGeneralTab(
        private val configuration: KotlinFacetConfiguration,
        private val editorContext: FacetEditorContext,
        private val validatorsManager: FacetValidatorsManager
) : FacetEditorTab() {
    class EditorComponent(
            private val project: Project,
            configuration: KotlinFacetConfiguration?
    ) : JPanel(BorderLayout()) {
        private val isMultiEditor = configuration == null

        private var editableCommonArguments: CommonCompilerArguments
        private var editableJvmArguments: K2JVMCompilerArguments
        private var editableJsArguments: K2JSCompilerArguments
        private var editableCompilerSettings: CompilerSettings

        val compilerConfigurable: KotlinCompilerConfigurableTab

        init {
            if (isMultiEditor) {
                editableCommonArguments = object : CommonCompilerArguments() {}
                editableJvmArguments = K2JVMCompilerArguments()
                editableJsArguments = K2JSCompilerArguments()
                editableCompilerSettings = CompilerSettings()


            }
            else {
                editableCommonArguments = configuration!!.settings.compilerArguments!!
                editableJvmArguments = editableCommonArguments as? K2JVMCompilerArguments
                                       ?: Kotlin2JvmCompilerArgumentsHolder.getInstance(project).settings
                editableJsArguments = editableCommonArguments as? K2JSCompilerArguments
                                      ?: Kotlin2JsCompilerArgumentsHolder.getInstance(project).settings
                editableCompilerSettings = configuration.settings.compilerSettings!!
            }

            compilerConfigurable = KotlinCompilerConfigurableTab(
                    project,
                    editableCommonArguments,
                    editableJsArguments,
                    editableJvmArguments,
                    editableCompilerSettings,
                    null,
                    false,
                    isMultiEditor
            )
        }

        val useProjectSettingsCheckBox = ThreeStateCheckBox("Use project settings").apply { isThirdStateEnabled = isMultiEditor }

        val targetPlatformComboBox =
                JComboBox<TargetPlatformKind<*>>(TargetPlatformKind.ALL_PLATFORMS.toTypedArray()).apply {
                    setRenderer(DescriptionListCellRenderer())
                }

        private val projectSettingsLink = HoverHyperlinkLabel("Edit project settings").apply {
            addHyperlinkListener {
                ShowSettingsUtilImpl.showSettingsDialog(project, compilerConfigurable.id, "")
                if (useProjectSettingsCheckBox.isSelected) {
                    updateCompilerConfigurable()
                }
            }
        }

        init {
            val contentPanel = FormBuilder
                    .createFormBuilder()
                    .addComponent(JPanel(BorderLayout()).apply {
                        add(useProjectSettingsCheckBox, BorderLayout.WEST)
                        add(projectSettingsLink, BorderLayout.EAST)
                    })
                    .addLabeledComponent("&Target platform: ", targetPlatformComboBox)
                    .addComponent(compilerConfigurable.createComponent()!!.apply {
                        border = null
                    })
                    .panel
                    .apply {
                        border = EmptyBorder(10, 10, 10, 10)
                    }
            add(contentPanel, BorderLayout.NORTH)

            useProjectSettingsCheckBox.addActionListener {
                updateCompilerConfigurable()
            }

            targetPlatformComboBox.addActionListener {
                updateCompilerConfigurable()
            }
        }

        internal fun updateCompilerConfigurable() {
            val useProjectSettings = useProjectSettingsCheckBox.isSelected
            compilerConfigurable.setTargetPlatform(chosenPlatform)
            compilerConfigurable.setEnabled(!useProjectSettings)
            if (useProjectSettings) {
                compilerConfigurable.commonCompilerArguments = KotlinCommonCompilerArgumentsHolder.getInstance(project).settings
                compilerConfigurable.k2jvmCompilerArguments = Kotlin2JvmCompilerArgumentsHolder.getInstance(project).settings
                compilerConfigurable.k2jsCompilerArguments = Kotlin2JsCompilerArgumentsHolder.getInstance(project).settings
                compilerConfigurable.compilerSettings = KotlinCompilerSettings.getInstance(project).settings
            }
            else {
                compilerConfigurable.commonCompilerArguments = editableCommonArguments
                compilerConfigurable.k2jvmCompilerArguments = editableJvmArguments
                compilerConfigurable.k2jsCompilerArguments = editableJsArguments
                compilerConfigurable.compilerSettings = editableCompilerSettings
            }
            compilerConfigurable.reset()
        }

        private val chosenPlatform: TargetPlatformKind<*>?
            get() = targetPlatformComboBox.selectedItem as TargetPlatformKind<*>?
    }

    inner class ArgumentConsistencyValidator : FacetEditorValidator() {
        override fun check(): ValidationResult {
            val platform = editor.targetPlatformComboBox.selectedItem as TargetPlatformKind<*>? ?: return ValidationResult.OK
            val primaryArguments = platform.createCompilerArguments().apply {
                editor.compilerConfigurable.applyTo(
                        this,
                        this as? K2JVMCompilerArguments ?: K2JVMCompilerArguments(),
                        this as? K2JSCompilerArguments ?: K2JSCompilerArguments(),
                        CompilerSettings()
                )
            }
            val argumentClass = primaryArguments.javaClass
            val additionalArguments = argumentClass.newInstance().apply {
                parseCommandLineArguments(splitArgumentString(editor.compilerConfigurable.additionalArgsOptionsField.text), this)
                validateArguments(errors)?.let { message -> return ValidationResult(message) }
            }
            val emptyArguments = argumentClass.newInstance()
            val fieldNamesToCheck = when (platform) {
                is TargetPlatformKind.Jvm -> jvmUIExposedFields
                is TargetPlatformKind.JavaScript -> jsUIExposedFields
                else -> commonUIExposedFields
            }

            val propertiesToCheck = collectProperties(argumentClass.kotlin, false).filter { it.name in fieldNamesToCheck }
            val overridingArguments = ArrayList<String>()
            val redundantArguments = ArrayList<String>()
            for (property in propertiesToCheck) {
                val additionalValue = property.get(additionalArguments)
                if (additionalValue != property.get(emptyArguments)) {
                    val argumentInfo = property.findAnnotation<Argument>() ?: continue
                    val addTo = if (additionalValue != property.get(primaryArguments)) overridingArguments else redundantArguments
                    addTo += "<strong>" + argumentInfo.value.first() + "</strong>"
                }
            }
            if (overridingArguments.isNotEmpty() || redundantArguments.isNotEmpty()) {
                val message = buildString {
                    if (overridingArguments.isNotEmpty()) {
                        append("Following arguments override facet settings: ${overridingArguments.joinToString()}")
                    }
                    if (redundantArguments.isNotEmpty()) {
                        if (isNotEmpty()) {
                            append("<br/>")
                        }
                        append("Following arguments are redundant: ${redundantArguments.joinToString()}")
                    }
                }
                return ValidationResult(message)
            }

            return ValidationResult.OK
        }
    }

    val editor = EditorComponent(editorContext.project, configuration)

    private val libraryValidator: FrameworkLibraryValidator
    private val coroutineValidator = ArgumentConsistencyValidator()

    private var enableValidation = false

    init {
        libraryValidator = FrameworkLibraryValidatorWithDynamicDescription(
                DelegatingLibrariesValidatorContext(editorContext),
                validatorsManager,
                "kotlin"
        ) { editor.targetPlatformComboBox.selectedItem as TargetPlatformKind<*> }

        validatorsManager.registerValidator(libraryValidator)
        validatorsManager.registerValidator(coroutineValidator)

        with(editor.compilerConfigurable) {
            reportWarningsCheckBox.validateOnChange()
            additionalArgsOptionsField.textField.validateOnChange()
            generateSourceMapsCheckBox.validateOnChange()
            outputPrefixFile.textField.validateOnChange()
            outputPostfixFile.textField.validateOnChange()
            outputDirectory.textField.validateOnChange()
            copyRuntimeFilesCheckBox.validateOnChange()
            moduleKindComboBox.validateOnChange()
            languageVersionComboBox.addActionListener {
                restrictAPIVersions()
                doValidate()
            }
            apiVersionComboBox.validateOnChange()
            coroutineSupportComboBox.validateOnChange()
        }
        editor.targetPlatformComboBox.validateOnChange()

        editor.updateCompilerConfigurable()
    }

    private fun restrictAPIVersions() {
        with(editor.compilerConfigurable) {
            val targetPlatform = editor.targetPlatformComboBox.selectedItem as TargetPlatformKind<*>?
            val libraryLevel = getLibraryLanguageLevel(editorContext.module, editorContext.rootModel, targetPlatform)
            val versionUpperBound = minOf(selectedLanguageVersion, libraryLevel)
            restrictAPIVersions(versionUpperBound)
        }
    }

    private fun JTextField.validateOnChange() {
        document.addDocumentListener(
                object : DocumentAdapter() {
                    override fun textChanged(e: DocumentEvent) {
                        doValidate()
                    }
                }
        )
    }

    private fun AbstractButton.validateOnChange() {
        addChangeListener { doValidate() }
    }

    private fun JComboBox<*>.validateOnChange() {
        addActionListener { doValidate() }
    }

    private fun validateOnce(body: () -> Unit) {
        enableValidation = false
        body()
        enableValidation = true
        doValidate()
    }

    private fun doValidate() {
        if (enableValidation) {
            validatorsManager.validate()
        }
    }

    override fun isModified(): Boolean {
        if (editor.useProjectSettingsCheckBox.isSelected != configuration.settings.useProjectSettings) return true
        if (editor.targetPlatformComboBox.selectedItem != configuration.settings.targetPlatformKind) return true
        return !editor.useProjectSettingsCheckBox.isSelected && editor.compilerConfigurable.isModified
    }

    override fun reset() {
        validateOnce {
            editor.useProjectSettingsCheckBox.isSelected = configuration.settings.useProjectSettings
            editor.targetPlatformComboBox.selectedItem = configuration.settings.targetPlatformKind
            editor.compilerConfigurable.reset()
            editor.updateCompilerConfigurable()
        }
    }

    override fun apply() {
        validateOnce {
            editor.compilerConfigurable.apply()
            with(configuration.settings) {
                useProjectSettings = editor.useProjectSettingsCheckBox.isSelected
                (editor.targetPlatformComboBox.selectedItem as TargetPlatformKind<*>?)?.let {
                    if (it != targetPlatformKind) {
                        val platformArguments = when (it) {
                            is TargetPlatformKind.Jvm -> editor.compilerConfigurable.k2jvmCompilerArguments
                            is TargetPlatformKind.JavaScript -> editor.compilerConfigurable.k2jsCompilerArguments
                            else -> null
                        }
                        compilerArguments = it.createCompilerArguments {
                            if (platformArguments != null) {
                                mergeBeans(platformArguments, this)
                            }
                            copyInheritedFields(compilerArguments!!, this)
                        }
                    }
                }
            }
        }
    }

    override fun getDisplayName() = "General"

    override fun createComponent(): JComponent {
        return editor
    }

    override fun disposeUIResources() {
        editor.compilerConfigurable.disposeUIResources()
    }
}
