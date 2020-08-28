package org.jetbrains.kotlin.tools.projectWizard.wizard

import com.intellij.facet.impl.ui.libraries.LibraryOptionsPanel
import com.intellij.framework.library.FrameworkLibraryVersionFilter
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory
import org.jetbrains.kotlin.idea.framework.JavaRuntimeLibraryDescription
import org.jetbrains.kotlin.tools.projectWizard.core.PluginsCreator
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingType
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.core.service.ServicesManager
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardService
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.ProjectTemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.wizard.service.IdeaWizardService
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class IdeWizard(
    createPlugins: PluginsCreator,
    initialServices: List<WizardService>,
    isUnitTestMode: Boolean
) : Wizard(
    createPlugins,
    ServicesManager(initialServices) { services ->
        services.firstOrNull { it is IdeaWizardService }
            ?: services.firstOrNull()
    },
    isUnitTestMode
) {
    init {
        initPluginSettingsDefaultValues()
    }

    val jpsData = JpsData(
        JavaRuntimeLibraryDescription(null),
        LibrariesContainerFactory.createContainer(null as Project?),
    )
    var jdk: Sdk? = null

    var projectPath by setting(StructurePlugin.projectPath.reference)
    var projectName by setting(StructurePlugin.name.reference)

    var groupId by setting(StructurePlugin.groupId.reference)
    var artifactId by setting(StructurePlugin.artifactId.reference)
    var buildSystemType by setting(BuildSystemPlugin.type.reference)

    var projectTemplate by setting(ProjectTemplatesPlugin.template.reference)

    private fun <V : Any, T : SettingType<V>> setting(reference: SettingReference<V, T>) =
        object : ReadWriteProperty<Any?, V?> {
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: V?) {
                if (value == null) return
                context.writeSettings {
                    reference.setValue(value)
                }
            }

            override fun getValue(thisRef: Any?, property: KProperty<*>): V? = context.read {
                reference.notRequiredSettingValue
            }
        }

    data class JpsData(
        val libraryDescription: JavaRuntimeLibraryDescription,
        val librariesContainer: LibrariesContainer,
        val libraryOptionsPanel: LibraryOptionsPanel = LibraryOptionsPanel(
            libraryDescription,
            "",
            FrameworkLibraryVersionFilter.ALL,
            librariesContainer,
            false
        )
    )
}

