package test

import kotlin.reflect.KProperty

annotation class Anno

@Anno val x: Int by object {
    fun getValue(thiz: Any?, data: KProperty<*>) = null!!
}
