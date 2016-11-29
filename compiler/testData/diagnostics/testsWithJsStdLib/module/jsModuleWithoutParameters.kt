// !DIAGNOSTICS: -NO_VALUE_FOR_PARAMETER, -CONSTANT_EXPECTED_TYPE_MISMATCH
// This should not crash
package foo

@JsModule @native fun foo(x: Int): Int

@JsModule(23) @native fun bar(x: Int): Int