// FIR_IDENTICAL
import kotlin.reflect.KMutableProperty0

class Module

class Context

class Model(
    private val value: KMutableProperty0<Module>,
    private val context: Context
)

abstract class Reference<V : Any> {
    abstract var v : V
}

class ModuleReference(m : Module) : Reference<Module>() {
    override var v : Module = m
}

abstract class SettingComponent<V: Any>(
    val reference: Reference<V>
) {
    var value: V
        get() = reference.v
        set(value) {
            reference.v = value
        }
}

class Component(
    reference: Reference<Module>,
    context: Context
) : SettingComponent<Module>(reference) {
    private val model = Model(::value, context)
}
