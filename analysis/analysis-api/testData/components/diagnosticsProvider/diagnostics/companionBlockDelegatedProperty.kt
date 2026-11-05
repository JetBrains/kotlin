// LANGUAGE: +CompanionBlocks +CompanionExtensions
import kotlin.reflect.KProperty

class StaticOnlyDelegate {
    operator fun getValue(thisRef: Nothing?, property: KProperty<*>): Int = 1
}

class Owner {
    companion {
        val delegatedProperty: Int by StaticOnlyDelegate()
    }
}

fun use(): Int = Owner.delegatedProperty
