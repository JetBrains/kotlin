// WITH_REFLECT

import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = 1
}

open class A {
    inline val z1 = 1

    val z1_1 = 1
        inline get() = field + 1

    inline var z2 = 1

    var z2_1 = 1
        inline set(p: Int) {}

    inline val z by Delegate()
}
