package org.jetbrains.kotlin.tools.projectWizard.wizard.ui

import org.jetbrains.kotlin.tools.projectWizard.core.context.ReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.context.WritingContext
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeContext
import org.jetbrains.kotlin.tools.projectWizard.core.context.SettingsWritingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.PluginSettingPropertyReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingType
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference

abstract class Component : Displayable {
    private val subComponents = mutableListOf<Component>()

    open fun onInit() {
        subComponents.forEach(Component::onInit)
    }

    protected fun <C : Component> C.asSubComponent(): C = also {
        this@Component.subComponents += it
    }
}

abstract class DynamicComponent(private val ideContext: IdeContext) : Component() {
    private var isInitialized: Boolean = false

    override fun onInit() {
        super.onInit()
        isInitialized = true
    }

    var <V : Any, T : SettingType<V>> SettingReference<V, T>.value: V?
        get() = read { notRequiredSettingValue() }
        set(value) = modify {
            value?.let { setValue(it) }
        }


    inline val <V : Any, reified T : SettingType<V>> PluginSettingPropertyReference<V, T>.value: V?
        get() = reference.value

    init {
        ideContext.eventManager.addSettingUpdaterEventListener { reference ->
            if (isInitialized) onValueUpdated(reference)
        }
    }

    protected fun <T> read(reader: ReadingContext.() -> T): T =
        reader(ideContext)

    protected fun <T> write(writer: WritingContext.() -> T): T =
        writer(ideContext)

    protected fun <T> modify(modifier: SettingsWritingContext.() -> T): T =
        modifier(ideContext)

    open fun onValueUpdated(reference: SettingReference<*, *>?) {}
}

interface FocusableComponent {
    fun focusOn() {}
}