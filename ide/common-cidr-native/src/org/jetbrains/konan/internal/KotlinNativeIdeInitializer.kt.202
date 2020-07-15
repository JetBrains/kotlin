package org.jetbrains.konan.internal

import com.intellij.codeInspection.InspectionEP
import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.runners.ProgramRunner
import com.intellij.ide.util.TipAndTrickBean
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.impl.ExtensionComponentAdapter
import com.intellij.psi.impl.search.IndexPatternBuilder
import com.intellij.psi.impl.search.JspIndexPatternBuilder
import com.intellij.task.ProjectTaskRunner
import com.intellij.task.impl.JpsProjectTaskRunner
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import java.util.function.BiPredicate
import java.util.function.Predicate

/**
 * @author Vladislav.Soroka
 */
@Suppress("SameParameterValue")
class KotlinNativeIdeInitializer {

    private companion object {
        val LOG = Logger.getInstance(KotlinNativeIdeInitializer::class.java)

        val PLUGINS_TO_UNREGISTER_TIP_AND_TRICKS = setOf(
            KotlinPluginUtil.KOTLIN_PLUGIN_ID.idString, // all tips & tricks that come from the main Kotlin plugin
            "org.intellij.intelliLang", // Java plugin specific
            "com.intellij.diagram" // Java plugin specific
        )

        val JAVA_PLUGIN_IDS = setOf(
            "com.intellij.kotlinNative.platformDeps", // Platform Deps (Java)
            "com.intellij.java" // Java
        )
    }

    init {
        // There are groovy local inspections which should not be loaded w/o groovy plugin enabled.
        // Those plugin definitions should become optional and dependant on groovy plugin.
        // This is a temp workaround before it happens.
        unregisterExtensionInstances(LocalInspectionEP.LOCAL_INSPECTION) {
            it.groupDisplayName == "Kotlin" && it.language == "Groovy"
        }

        // Suppress irrelevant tips & tricks
        unregisterExtensionsFromPlugins(TipAndTrickBean.EP_NAME, PLUGINS_TO_UNREGISTER_TIP_AND_TRICKS)

        // Disable JPS
        unregisterExtensionsByClass(ProjectTaskRunner.EP_NAME, JpsProjectTaskRunner::class.java)

        // Disable run configurations provided by Java plugin
        unregisterExtensionsFromPlugins(ConfigurationType.CONFIGURATION_TYPE_EP, JAVA_PLUGIN_IDS)
        unregisterExtensionsFromPlugins(RunConfigurationProducer.EP_NAME, JAVA_PLUGIN_IDS)
        unregisterExtensionsFromPlugins(ProgramRunner.PROGRAM_RUNNER_EP, JAVA_PLUGIN_IDS)

        // Disable Java Server Pages indexes
        unregisterExtensionsByClass(IndexPatternBuilder.EP_NAME, JspIndexPatternBuilder::class.java)
    }

    private fun <T : Any> unregisterExtensionsByClass(
        extensionPointName: ExtensionPointName<T>,
        extensionClass: Class<out T>
    ) {
        val extensionPoint = Extensions.getRootArea().getExtensionPoint(extensionPointName)
        unregisterExtensions(extensionPoint) { className, _ -> className == extensionClass.name }
    }

    private fun <T : Any> unregisterExtensionsFromPlugins(
        extensionPointName: ExtensionPointName<T>,
        pluginIds: Set<String>
    ) {
        val extensionPoint = Extensions.getRootArea().getExtensionPoint(extensionPointName)
        unregisterExtensions(extensionPoint) { _, adapter -> adapter.pluginDescriptor.pluginId?.idString in pluginIds }
    }

    private fun <T : Any> unregisterExtensions(
        extensionPoint: ExtensionPoint<T>,
        predicate: (String, ExtensionComponentAdapter) -> Boolean
    ) {
        extensionPoint.unregisterExtensions(predicate.wrap(extensionPoint.toString()), false)
    }

    // TODO: drop this method as it forces all extensions to instantiate and then unregisters some of them.
    @Suppress("DEPRECATION")
    private fun <T : InspectionEP> unregisterExtensionInstances(
            extensionPointName: ExtensionPointName<T>,
            predicate: (T) -> Boolean
    ) {
        val extensionPoint = Extensions.getRootArea().getExtensionPoint(extensionPointName)
        extensionPoint.unregisterExtensions(predicate.wrap(extensionPointName.name))
    }

    private fun <T : InspectionEP> ((T) -> Boolean).wrap(extensionPointName: String) =
        Predicate<T> { extension ->
            this(extension).also { result ->
                if (result) LOG.warn("unregistering extension $extensionPointName, ${extension::class.java}, ${extension.pluginDescriptor}")
            }
        }//.negate()

    private fun ((String, ExtensionComponentAdapter) -> Boolean).wrap(extensionPointName: String) =
        BiPredicate<String, ExtensionComponentAdapter> { className, adapter ->
            this(className, adapter).also { result ->
                if (result) LOG.warn("unregistering extension $extensionPointName, $className, ${adapter.pluginDescriptor}")
            }
        }.negate()
}
