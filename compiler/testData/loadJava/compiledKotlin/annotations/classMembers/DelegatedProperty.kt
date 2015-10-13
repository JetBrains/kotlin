package test

import kotlin.reflect.KProperty

annotation class Anno

class Class {
    @Anno val x: Int by object {
        fun getValue(thiz: Class, data: KProperty<*>) = null!!
    }
}
