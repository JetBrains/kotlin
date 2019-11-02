// TARGET_BACKEND: JVM

// WITH_REFLECT
// FULL_JDK

import java.lang.reflect.InvocationTargetException
import kotlin.reflect.*

object Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): String {
        (p as? KProperty0<String>)?.get()
        (p as? KProperty1<O, String>)?.get(O)
        (p as? KProperty2<O, O, String>)?.get(O, O)
        return "Fail"
    }

    operator fun setValue(t: Any?, p: KProperty<*>, v: String) {
        (p as? KMutableProperty0<String>)?.set(v)
        (p as? KMutableProperty1<O, String>)?.set(O, v)
        (p as? KMutableProperty2<O, O, String>)?.set(O, O, v)
    }
}

var topLevel: String by Delegate
object O {
    var member: String by Delegate
    var O.memExt: String by Delegate
}

fun check(lambda: () -> Unit) {
    try {
        lambda()
    } catch (e: Throwable) {
        if (e !is InvocationTargetException && e !is StackOverflowError) {
            throw RuntimeException("The current implementation uses reflection to get the value of the property," +
                                   "so either InvocationTargetException or StackOverflowError should have happened",
                                   e)
        }
        return
    }
    throw AssertionError("Getting the property value with .get() from getValue() or setting it with .set() in setValue() " +
                         "is effectively an endless recursion and should fail")
}

fun box(): String {
    check { topLevel }
    check { topLevel = "" }
    check { O.member }
    check { O.member = "" }
    with (O) {
        check { O.memExt }
        check { O.memExt = "" }
    }

    return "OK"
}
