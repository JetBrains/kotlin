// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.assertEquals

var foo = ""
var String.bar: String
    get() = this
    set(value) {}

class A(var baz: Int) {
    var String.quux: String
        get() = this
        set(value) {}
}

fun box(): String {
    assertEquals("<get-foo>", ::foo.getter.name)
    assertEquals("<set-foo>", ::foo.setter.name)

    assertEquals("<get-bar>", String::bar.getter.name)
    assertEquals("<set-bar>", String::bar.setter.name)

    assertEquals("<get-baz>", A::baz.getter.name)
    assertEquals("<set-baz>", A::baz.setter.name)

    val me = A::class.memberExtensionProperties.single() as KMutableProperty2<A, String, String>
    assertEquals("<get-quux>", me.getter.name)
    assertEquals("<set-quux>", me.setter.name)

    return "OK"
}
