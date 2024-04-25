// JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8

import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): String = p.name
}

interface Foo {

    fun test(): String {
        val O by Delegate()
        return O
    }
}

interface Foo2: Foo {

    override fun test(): String {
        val K by Delegate()
        return super.test() + K
    }
}

fun box(): String {
    return object : Foo2 {}.test()
}