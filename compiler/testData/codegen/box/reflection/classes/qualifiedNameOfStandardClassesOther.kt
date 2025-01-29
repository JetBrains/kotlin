// WASM_ALLOW_FQNAME_IN_KCLASS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JVM_IR
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// ^^^ KT-74799: extra FUNCTION_INTERFACE_CLASS modifier on class reference to Function42

import kotlin.test.assertEquals

fun box(): String {
    assertEquals("kotlin.Array", Array::class.qualifiedName)
    assertEquals("kotlin.collections.MutableList", MutableList::class.qualifiedName)
    assertEquals("kotlin.collections.MutableMap.MutableEntry", MutableMap.MutableEntry::class.qualifiedName)

    assertEquals("kotlin.Function42", Function42::class.qualifiedName)

    return "OK"
}
