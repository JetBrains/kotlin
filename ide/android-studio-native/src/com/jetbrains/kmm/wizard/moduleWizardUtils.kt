package com.jetbrains.kmm.wizard

import com.android.tools.adtui.LabelWithEditButton
import com.android.tools.idea.npw.labelFor
import com.android.tools.idea.npw.model.NewProjectModel
import com.android.tools.idea.observable.BindingsManager
import com.android.tools.idea.observable.ListenerManager
import com.android.tools.idea.observable.core.BoolProperty
import com.android.tools.idea.observable.core.BoolValueProperty
import com.android.tools.idea.observable.expressions.Expression
import com.android.tools.idea.observable.ui.SelectedProperty
import com.android.tools.idea.observable.ui.TextProperty
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.RecipeExecutor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.panel
import com.jetbrains.kmm.KMM_LOG
import com.jetbrains.kmm.KmmBundle
import com.jetbrains.kmm.wizard.templates.*
import org.jetbrains.android.refactoring.getProjectProperties
import javax.swing.JTextField


private const val HMMP_SUPPORT_KEY = "kotlin.mpp.enableGranularSourceSetsMetadata"
private const val COMMONIZER_DISABLE_KEY = "kotlin.native.enableDependencyPropagation"

private fun String.asDirectory(): String = this.replace(".", "/")


fun moduleStepDialogPanel(
    moduleName: JTextField,
    packageName: LabelWithEditButton,
    packageNameField: JTextField,
    generateTestsCheck: JBCheckBox,
    generatePackTaskCheck: JBCheckBox
): DialogPanel = panel {
    row {
        cell {
            labelFor(KmmBundle.message("wizard.module.moduleNameLabel"), moduleName)
        }
        moduleName()
    }

    row {
        labelFor(KmmBundle.message("wizard.module.packageNameLabel"), packageName)
        packageNameField()
    }

    row {
        cell {
            generateTestsCheck()
        }
    }

    row {
        cell {
            generatePackTaskCheck()
        }
    }
}


fun bindModuleStepSettings(
    bindings: BindingsManager,
    listeners: ListenerManager,
    model: KmmModuleModel,
    packageNameField: JTextField,
    generateTestsCheck: JBCheckBox,
    generatePackTaskCheck: JBCheckBox
) {
    bindings.bindTwoWay(SelectedProperty(generateTestsCheck), model.generateTests)
    bindings.bindTwoWay(SelectedProperty(generatePackTaskCheck), model.generatePackTask)

    val isPackageNameSynced: BoolProperty = BoolValueProperty(true)

    val basePackage = NewProjectModel.getSuggestedProjectPackage()
    val computedPackageName: Expression<String> = object : Expression<String>(model.moduleName) {
        override fun get() = "${basePackage}.${NewProjectModel.nameToJavaPackage(model.moduleName.get())}"
    }

    val packageNameText = TextProperty(packageNameField)
    bindings.bind(model.packageName, packageNameText)
    bindings.bind(packageNameText, computedPackageName, isPackageNameSynced)

    listeners.listen(packageNameText) { value: String -> isPackageNameSynced.set(value == computedPackageName.get()) }
}


fun RecipeExecutor.generateKmmModule(project: Project, data: ModuleTemplateData, generateTests: Boolean, generatePackTask: Boolean) {
    val packageName = data.packageName
    val moduleDir = data.rootDir
    val srcDir = moduleDir.resolve("src")

    addIncludeToSettings(data.name)

    val propertiesFile = project.getProjectProperties(true)

    if (propertiesFile != null) {
        propertiesFile.findPropertyByKey(HMMP_SUPPORT_KEY)?.setValue("true") ?: propertiesFile.addProperty(HMMP_SUPPORT_KEY, "true")
        propertiesFile.findPropertyByKey(COMMONIZER_DISABLE_KEY)?.setValue("false")
            ?: propertiesFile.addProperty(COMMONIZER_DISABLE_KEY, "false")
    } else {
        KMM_LOG.error("Failed to update gradle.properties during KMM module instantiation")
    }

    val templates = mutableListOf(CommonMainPlatformKt, CommonMainGreetingKt, IosMainPlatformKt, AndroidMainPlatformKt)

    if (generateTests) {
        templates += listOf(IosTestKt, AndroidTestKt)
    }

    for (template in templates) {
        val sourceSetOut = srcDir.resolve(template.sourceSet).resolve("kotlin").resolve(packageName.asDirectory())
        createDirectory(sourceSetOut)
        save(template.render(packageName), sourceSetOut.resolve(template.fileName))
    }

    val entryPointsPackage = "$packageName.android"

    createDirectory(srcDir.resolve("androidMain/kotlin/" + entryPointsPackage.asDirectory()))
    save(buildFileKts(data.name, generateTests, generatePackTask), moduleDir.resolve("build.gradle.kts"))
    save(androidManifestXml(entryPointsPackage), srcDir.resolve("androidMain/AndroidManifest.xml"))
}
