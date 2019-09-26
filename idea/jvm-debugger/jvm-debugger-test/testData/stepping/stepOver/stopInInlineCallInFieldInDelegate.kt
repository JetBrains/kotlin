package stopInInlineCallInFieldInDelegate

import kotlin.reflect.KProperty

class A {
    var value = 42

    val b: String by getDelegate {
        {
            //Breakpoint!
            foo(value)
        }()
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun getDelegate(function: () -> Unit): Delegate {
    function()
    return Delegate()
}

class Delegate() {
    operator fun getValue(a: A, property: KProperty<*>): String = ""
}

fun foo(a: Any) {}

fun main(args: Array<String>) {
    A().b
}