// !DIAGNOSTICS: -NO_VALUE_FOR_PARAMETER, -CONSTANT_EXPECTED_TYPE_MISMATCH
// This should not crash, see KT-14752
package foo

@JsName fun foo(x: Int) = x

@JsName(<!ARGUMENT_TYPE_MISMATCH!>23<!>) fun bar(x: Int) = x
