// Container
import kotlin.reflect.KProperty

class Container {
    companion object {
        fun <R> delegate(): Delegate<R> = null!!
    }

    interface Delegate<R> {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): R = null!!

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: R) {
        }
    }

    abstract class Base {
        val a: String by delegate()
        var mutable: String? by delegate()
        open val b: String by delegate()
        open val c: String = ""
        abstract val d: String
    }

    class Derived : Base() {
        override val b: String by delegate()
        override val c: String by delegate()
        override val d: String by delegate()
    }
}
