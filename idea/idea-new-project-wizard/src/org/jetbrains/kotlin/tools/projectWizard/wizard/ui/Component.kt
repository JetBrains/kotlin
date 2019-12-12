package org.jetbrains.kotlin.tools.projectWizard.wizard.ui

import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*

abstract class Component : Displayable {
    private val subComponents = mutableListOf<Component>()

    open fun onInit() {
        subComponents.forEach(Component::onInit)
    }

    protected fun <C : Component> C.asSubComponent(): C = also {
        this@Component.subComponents += it
    }
}

abstract class DynamicComponent(private val valuesReadingContext: ValuesReadingContext) : Component() {
    //TODO do not use it in future
    protected val context = valuesReadingContext.context
    protected val eventManager
        get() = valuesReadingContext.context.eventManager

    private var isInitialized: Boolean = false

    override fun onInit() {
        super.onInit()
        isInitialized = true
    }

    var <V : Any, T : SettingType<V>> SettingReference<V, T>.value: V?
        get() = with(valuesReadingContext) { notRequiredSettingValue() }
        set(value) {
            with(context) { settingContext[this@value] = value!! }
        }

    inline val <V : Any, reified T : SettingType<V>> PluginSettingPropertyReference<V, T>.value: V?
        get() = reference.value

    init {
        valuesReadingContext.context.eventManager.addSettingUpdaterEventListener { reference ->
            if (isInitialized) onValueUpdated(reference)
        }
    }
    open fun onValueUpdated(reference: SettingReference<*, *>?) {}
}