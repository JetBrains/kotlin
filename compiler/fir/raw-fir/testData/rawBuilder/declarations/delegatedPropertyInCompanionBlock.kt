// IGNORE_BODY_CALCULATOR: KT-91441
// LANGUAGE: +CompanionBlocks
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = 1
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {}
}

class Owner {
    companion {
        val staticVal: Int by Delegate()
        var staticVar: Int by Delegate()
    }

    val instanceVal: Int by Delegate()
    var instanceVar: Int by Delegate()
}
