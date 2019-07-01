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

import com.intellij.facet.ui.*
import com.intellij.ide.actions.ShowSettingsUtilImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.HoverHyperlinkLabel
import com.intellij.util.ui.CheckBox
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.ThreeStateCheckBox
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.createArguments
import org.jetbrains.kotlin.config.splitArgumentString
import org.jetbrains.kotlin.idea.compiler.configuration.*
import org.jetbrains.kotlin.idea.core.util.onTextChange
import org.jetbrains.kotlin.platform.*
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.border.EmptyBorder
import kotlin.reflect.full.findAnnotation

class KotlinFacetEditorGeneralTab(
        private val configuration: KotlinFacetConfiguration,
        private val editorContext: FacetEditorContext,
        private val validatorsManager: FacetValidatorsManager
) : FacetEditorTab() {
    class EditorComponent(
            private val project: Project,
            private val configuration: KotlinFacetConfiguration?
    ) : JPanel(BorderLayout()) {
        private val isMultiEditor: Boolean
            get() = configuration == null

        private lateinit var editableCommonArguments: CommonCompilerArguments
        private lateinit var editableJvmArguments: K2JVMCompilerArguments
        private lateinit var editableJsArguments: K2JSCompilerArguments
        private lateinit var editableCompilerSettings: CompilerSettings

        lateinit var compilerConfigurable: KotlinCompilerConfigurableTab
            private set

        lateinit var useProjectSettingsCheckBox: ThreeStateCheckBox

        lateinit var platformToCheckbox: Map<SimplePlatform, ThreeStateCheckBox>

        private lateinit var projectSettingsLink: HoverHyperlinkLabel

        fun initialize() {
            if (isMultiEditor) {
                editableCommonArguments = object : CommonCompilerArguments() {}
                editableJvmArguments = K2JVMCompilerArguments()
                editableJsArguments = K2JSCompilerArguments()
                editableCompilerSettings = CompilerSettings()
            } else {
                editableCommonArguments = configuration!!.settings.compilerArguments!!
                editableJvmArguments = editableCommonArguments as? K2JVMCompilerArguments
                        ?: Kotlin2JvmCompilerArgumentsHolder.getInstance(project).settings.unfrozen() as K2JVMCompilerArguments
                editableJsArguments = editableCommonArguments as? K2JSCompilerArguments
                        ?: Kotlin2JsCompilerArgumentsHolder.getInstance(project).settings.unfrozen() as K2JSCompilerArguments
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

            useProjectSettingsCheckBox = ThreeStateCheckBox("Use project settings").apply { isThirdStateEnabled = isMultiEditor }
            platformToCheckbox = CommonPlatforms
                .allSimplePlatforms
                .flatMap { it.componentPlatforms }
                .distinct()
                .sortedBy { it.platformName }
                .associate { Pair(it, ThreeStateCheckBox(it.platformName).apply {isThirdStateEnabled = isMultiEditor} ) }
            projectSettingsLink = HoverHyperlinkLabel("Edit project settings").apply {
                addHyperlinkListener {
                    ShowSettingsUtilImpl.showSettingsDialog(project, compilerConfigurable.id, "")
                    if (useProjectSettingsCheckBox.isSelected) {
                        updateCompilerConfigurable()
                    }
                }
            }

            val targetPlatformsPanel = JPanel()
            platformToCheckbox.values.forEach {
                targetPlatformsPanel.add(it)
                it.isEnabled = true //TODO(auskov): think about enabling/disabling editing facet settings
            }
            val contentPanel = FormBuilder
                    .createFormBuilder()
                    .addComponent(JPanel(BorderLayout()).apply {
                        add(useProjectSettingsCheckBox, BorderLayout.WEST)
                        add(projectSettingsLink, BorderLayout.EAST)
                    }).addLabeledComponent("Selected target platforms:", targetPlatformsPanel)
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

            platformToCheckbox.values.forEach {
                it.addActionListener {
                    updateCompilerConfigurable()
                }
            }
        }

        internal fun updateCompilerConfigurable() {
            val useProjectSettings = useProjectSettingsCheckBox.isSelected
            compilerConfigurable.setTargetPlatform(getChosenPlatform()?.idePlatformKind)
            compilerConfigurable.setEnabled(!useProjectSettings)
            if (useProjectSettings) {
                compilerConfigurable.commonCompilerArguments = KotlinCommonCompilerArgumentsHolder.getInstance(project).settings.unfrozen() as CommonCompilerArguments?
                compilerConfigurable.k2jvmCompilerArguments = Kotlin2JvmCompilerArgumentsHolder.getInstance(project).settings.unfrozen() as K2JVMCompilerArguments?
                compilerConfigurable.k2jsCompilerArguments = Kotlin2JsCompilerArgumentsHolder.getInstance(project).settings.unfrozen() as K2JSCompilerArguments?
                compilerConfigurable.compilerSettings = KotlinCompilerSettings.getInstance(project).settings.unfrozen() as CompilerSettings?
            }
            else {
                compilerConfigurable.commonCompilerArguments = editableCommonArguments
                compilerConfigurable.k2jvmCompilerArguments = editableJvmArguments
                compilerConfigurable.k2jsCompilerArguments = editableJsArguments
                compilerConfigurable.compilerSettings = editableCompilerSettings
            }
            compilerConfigurable.reset()
        }

        fun getChosenPlatform(): TargetPlatform? {
            val simplePlatforms = platformToCheckbox.filter { it.value.isSelected }.map { it.key }.toSet()
            return if (simplePlatforms.isEmpty()) null else TargetPlatform(simplePlatforms)
        }
    }

    inner class ArgumentConsistencyValidator : FacetEditorValidator() {
        override fun check(): ValidationResult {
            val platform = editor.getChosenPlatform() ?: return ValidationResult("At least one target platform should be selected")
            val primaryArguments = platform.createArguments {
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
            val fieldNamesToCheck = when {
                platform.isJvm() -> jvmUIExposedFields
                platform.isJs() -> jsUIExposedFields
                platform.isCommon() -> metadataUIExposedFields
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

    private var isInitialized = false
    val editor by lazy { EditorComponent(editorContext.project, configuration) }

    private var enableValidation = false

    private fun onLanguageLevelChanged() {
        with(editor.compilerConfigurable) {
            onLanguageLevelChanged(selectedLanguageVersionView)
        }
    }

    private fun JTextField.validateOnChange() {
        onTextChange { doValidate() }
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

    fun initializeIfNeeded() {
        if (isInitialized) return

        editor.initialize()

        for (creator in KotlinFacetValidatorCreator.EP_NAME.getExtensions()) {
            validatorsManager.registerValidator(creator.create(editor, validatorsManager, editorContext))
        }

        validatorsManager.registerValidator(ArgumentConsistencyValidator())

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
                onLanguageLevelChanged()
                doValidate()
            }
            apiVersionComboBox.validateOnChange()
            coroutineSupportComboBox.validateOnChange()
        }
        editor.platformToCheckbox.values.forEach {
            it.validateOnChange()
        }

        editor.updateCompilerConfigurable()
        isInitialized = true

        reset()
    }

    override fun onTabEntering() {
        initializeIfNeeded()
    }

    override fun isModified(): Boolean {
        if (!isInitialized) return false
        if (editor.useProjectSettingsCheckBox.isSelected != configuration.settings.useProjectSettings) return true
        if (editor.getChosenPlatform() != configuration.settings.targetPlatform) return true
        return !editor.useProjectSettingsCheckBox.isSelected && editor.compilerConfigurable.isModified
    }

    override fun reset() {
        if (!isInitialized) return
        validateOnce {
            editor.useProjectSettingsCheckBox.isSelected = configuration.settings.useProjectSettings
            editor.platformToCheckbox.forEach {
                it.value.isSelected = configuration.settings.targetPlatform?.contains(it.key) ?: false
            }
            editor.compilerConfigurable.reset()
            editor.updateCompilerConfigurable()
        }
    }

    override fun apply() {
        initializeIfNeeded()
        validateOnce {
            editor.compilerConfigurable.apply()
            with(configuration.settings) {
                useProjectSettings = editor.useProjectSettingsCheckBox.isSelected
                editor.getChosenPlatform()?.let {
                    if (it != targetPlatform) {
                        val platformArguments = when {
                            it.isJvm() -> editor.compilerConfigurable.k2jvmCompilerArguments
                            it.isJs() -> editor.compilerConfigurable.k2jsCompilerArguments
                            else -> null
                        }
                        compilerArguments = it.createArguments {
                            if (platformArguments != null) {
                                mergeBeans(platformArguments, this)
                            }
                            copyInheritedFields(compilerArguments!!, this)
                        }
                    }
                }
                updateMergedArguments()
            }
        }
    }

    override fun getDisplayName() = "General"

    override fun createComponent(): JComponent {
        return editor
    }

    override fun disposeUIResources() {
        if (isInitialized) {
            editor.compilerConfigurable.disposeUIResources()
        }
    }
}

@Suppress("UNCHECKED_CAST")
val <T> ComboBox<T>.selectedItemTyped: T? get() = selectedItem as T?
