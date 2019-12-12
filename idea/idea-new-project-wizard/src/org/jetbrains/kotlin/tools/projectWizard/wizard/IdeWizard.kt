package org.jetbrains.kotlin.tools.projectWizard.wizard

import org.jetbrains.kotlin.tools.projectWizard.core.PluginsCreator
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingType
import org.jetbrains.kotlin.tools.projectWizard.core.entity.reference
import org.jetbrains.kotlin.tools.projectWizard.core.service.Service
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class IdeWizard(
    createPlugins: PluginsCreator,
    initialServices: List<Service>
) : Wizard(createPlugins, initialServices) {
    private val allSettings = plugins.flatMap { it.declaredSettings }

    init {
        context.settingContext.initPluginSettings(allSettings)
    }

    var projectPath by setting(StructurePlugin::projectPath.reference)
    var projectName by setting(StructurePlugin::name.reference)

    var groupId by setting(StructurePlugin::groupId.reference)
    var artifactId by setting(StructurePlugin::artifactId.reference)
    var buildSystemType by setting(BuildSystemPlugin::type.reference)

    private fun <V : Any, T : SettingType<V>> setting(reference: SettingReference<V, T>) =
        object : ReadWriteProperty<Any?, V?> {
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: V?) {
                context.settingContext[reference] = value ?: return
            }

            @Suppress("UNCHECKED_CAST")
            override fun getValue(thisRef: Any?, property: KProperty<*>): V? =
                context.settingContext[reference]
        }
}

