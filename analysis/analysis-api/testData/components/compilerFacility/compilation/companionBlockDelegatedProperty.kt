// LANGUAGE: +CompanionBlocks +CompanionExtensions
// FILE: main.kt
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = 1
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {}
}

class Owner {
    companion {
        val lazyProperty: Int by lazy { 1 }
        var delegatedProperty: Int by Delegate()
    }
}

fun use(): Int = Owner.lazyProperty + Owner.delegatedProperty
