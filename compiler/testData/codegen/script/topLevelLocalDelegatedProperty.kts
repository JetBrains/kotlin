// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = 3
}


val prop: Int by Delegate()

val  x = prop

// expected: x: 3
