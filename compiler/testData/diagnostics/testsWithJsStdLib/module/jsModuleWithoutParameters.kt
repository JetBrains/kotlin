// !DIAGNOSTICS: -NO_VALUE_FOR_PARAMETER, -CONSTANT_EXPECTED_TYPE_MISMATCH
// This should not crash
package foo

@JsModule
external fun foo(x: Int): Int

@JsModule(23)
external fun bar(x: Int): Int