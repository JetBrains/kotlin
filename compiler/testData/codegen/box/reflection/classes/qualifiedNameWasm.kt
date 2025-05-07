// TARGET_BACKEND: WASM
// WASM_ALLOW_FQNAME_IN_KCLASS

package test

import kotlin.test.assertEquals

fun box(): String {
    // TODO: KT-71517 K/Wasm: KClass::qualifiedName for local classes and objects returns non-null value
    class Local
    assertEquals("test.Local", Local::class.qualifiedName)

    val o = object {}
    assertEquals("test.<no name provided>", o::class.qualifiedName)
    return "OK"
}
