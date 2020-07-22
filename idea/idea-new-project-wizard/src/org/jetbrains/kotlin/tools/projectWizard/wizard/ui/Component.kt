package org.jetbrains.kotlin.tools.projectWizard.wizard.ui


import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.Reader
import org.jetbrains.kotlin.tools.projectWizard.core.SettingsWriter
import org.jetbrains.kotlin.tools.projectWizard.core.Writer
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingType

abstract class Component : Displayable, ErrorNavigatable {
    private val subComponents = mutableListOf<Component>()

    open fun onInit() {
        subComponents.forEach(Component::onInit)
    }

    protected fun <C : Component> C.asSubComponent(): C = also {
        this@Component.subComponents += it
    }

    protected fun clearSubComponents() {
        subComponents.clear()
    }

    override fun navigateTo(error: ValidationResult.ValidationError) {
        subComponents.forEach { it.navigateTo(error) }
    }
}

abstract class DynamicComponent(private val context: Context) : Component() {
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

    init {
        write {
            eventManager.addSettingUpdaterEventListener { reference ->
                if (isInitialized) onValueUpdated(reference)
            }
        }
    }

    protected fun <T> read(reader: Reader.() -> T): T =
        context.read(reader)

    protected fun <T> write(writer: Writer.() -> T): T =
        context.write(writer)

    protected fun <T> modify(modifier: SettingsWriter.() -> T): T =
        context.writeSettings(modifier)

    open fun onValueUpdated(reference: SettingReference<*, *>?) {}
}

abstract class TitledComponent(context: Context) : DynamicComponent(context) {
    open val forceLabelCenteringOffset: Int? = null
    open val additionalComponentPadding: Int = 0
    open val maximumWidth: Int? = null
    abstract val title: String?
    open fun shouldBeShow(): Boolean = true
}

interface FocusableComponent {
    fun focusOn() {}
}

interface ErrorNavigatable {
    fun navigateTo(error: ValidationResult.ValidationError)
}