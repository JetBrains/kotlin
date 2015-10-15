// FULL_JDK

import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.test.assertFailsWith

class Delegate {
    fun getValue(t: Any?, p: KProperty<*>): String {
        return (p as KProperty0<String>).get()
    }
}

val prop: String by Delegate()

fun box(): String {
    assertFailsWith(
            StackOverflowError::class.java,
            "Getting the property value with .get() from getValue is effectively an endless recursion and should fail"
    ) {
        prop
    }

    return "OK"
}
