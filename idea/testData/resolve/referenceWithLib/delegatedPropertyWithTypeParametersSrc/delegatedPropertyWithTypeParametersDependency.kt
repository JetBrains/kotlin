package dependency

import kotlin.reflect.KProperty

public fun <T, R> T.getValue(thisRef: R, desc: KProperty<*>): Int {
    return 3
}

public fun <T, R> T.setValue(thisRef: R, desc: KProperty<*>, value: Int) {
}
