// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND: JS, NATIVE, WASM

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*

annotation class Foo(vararg val strings: String)

annotation class Bar(vararg val bytes: Byte)

fun box(): String {
    val fooConstructor = Foo::class.primaryConstructor!!
    val foo = fooConstructor.callBy(emptyMap())
    assert(foo.strings.isEmpty())

    val barConstructor = Bar::class.primaryConstructor!!
    val bar = barConstructor.callBy(emptyMap())
    assert(bar.bytes.isEmpty())

    return "OK"
}
