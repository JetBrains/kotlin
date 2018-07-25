// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

package zzz
import java.lang.reflect.Field
import kotlin.test.assertEquals
import kotlin.reflect.KProperty0

@JvmField public val publicField = "1";
@JvmField internal val internalField = "2";

fun testAccessors() {
    val kProperty: KProperty0<String> = ::publicField
    checkAccessor(kProperty, "1")
    checkAccessor(::internalField, "2")
}


fun box(): String {
    testAccessors()
    return "OK"
}

public fun <T, R> checkAccessor(prop: KProperty0<T>, value: R) {
    assertEquals<Any?>(prop.get(), value, "Property ${prop} has wrong value")
}
