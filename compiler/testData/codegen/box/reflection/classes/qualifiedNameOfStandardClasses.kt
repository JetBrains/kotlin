// WITH_STDLIB
// WASM_ALLOW_FQNAME_IN_KCLASS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// ^^^ KT-74799: extra FUNCTION_INTERFACE_CLASS modifier on class reference to Function0, Function1, Function5

import kotlin.test.assertEquals

fun box(): String {
    assertEquals("kotlin.Any", Any::class.qualifiedName)
    assertEquals("kotlin.String", String::class.qualifiedName)
    assertEquals("kotlin.CharSequence", CharSequence::class.qualifiedName)
    assertEquals("kotlin.Number", Number::class.qualifiedName)
    assertEquals("kotlin.Int", Int::class.qualifiedName)
    assertEquals("kotlin.Long", Long::class.qualifiedName)

    assertEquals("kotlin.IntArray", IntArray::class.qualifiedName)
    assertEquals("kotlin.DoubleArray", DoubleArray::class.qualifiedName)

    assertEquals("kotlin.Int.Companion", Int.Companion::class.qualifiedName)
    assertEquals("kotlin.Double.Companion", Double.Companion::class.qualifiedName)
    assertEquals("kotlin.Char.Companion", Char.Companion::class.qualifiedName)

    assertEquals("kotlin.ranges.IntRange", IntRange::class.qualifiedName)

    assertEquals("kotlin.collections.List", List::class.qualifiedName)
    assertEquals("kotlin.collections.Map.Entry", Map.Entry::class.qualifiedName)

    assertEquals("kotlin.Function0", Function0::class.qualifiedName)
    assertEquals("kotlin.Function1", Function1::class.qualifiedName)
    assertEquals("kotlin.Function5", Function5::class.qualifiedName)

    return "OK"
}
