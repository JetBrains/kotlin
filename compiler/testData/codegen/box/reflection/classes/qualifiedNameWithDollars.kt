// WITH_STDLIB
// WASM_ALLOW_FQNAME_IN_KCLASS
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR NATIVE
// ^^^ Source code is not compiled in JS, Native.

package test

import kotlin.test.assertEquals

class Klass {
    class `Nested$With$Dollars`
}

class AnotherKlass {
    object `Nested$With$Dollars`
}

fun box(): String {
    assertEquals("test.Klass.Nested\$With\$Dollars", Klass.`Nested$With$Dollars`::class.qualifiedName)
    assertEquals("test.AnotherKlass.Nested\$With\$Dollars", AnotherKlass.`Nested$With$Dollars`::class.qualifiedName)

    return "OK"
}
