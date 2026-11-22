package inline

import kotlin.reflect.KProperty

inline operator fun Inline.getValue(receiver: Any?, prop: KProperty<*>): Int {
    return 0
}
