// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.assert

annotation class Foo(vararg val strings: String)

fun box(): String {
    val constructor = Foo::class.primaryConstructor!!
    val annotation = constructor.callBy(emptyMap())
    assert(annotation.strings.isEmpty())
    return "OK"
}
