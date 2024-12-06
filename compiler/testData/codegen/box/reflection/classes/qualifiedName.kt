// WITH_STDLIB
// WASM_ALLOW_FQNAME_IN_KCLASS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

package test

import kotlin.test.assertEquals

class Klass {
    class Nested
    companion object
}

class AnotherKlass {
    object Nested
    companion object Default
}

fun box(): String {
    assertEquals("test.Klass", Klass::class.qualifiedName)
    assertEquals("test.Klass.Nested", Klass.Nested::class.qualifiedName)
    assertEquals("test.Klass.Companion", Klass.Companion::class.qualifiedName)

    assertEquals("test.AnotherKlass", AnotherKlass::class.qualifiedName)
    assertEquals("test.AnotherKlass.Nested", AnotherKlass.Nested::class.qualifiedName)
    assertEquals("test.AnotherKlass.Default", AnotherKlass.Default::class.qualifiedName)

    return "OK"
}
