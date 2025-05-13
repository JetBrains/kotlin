// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-76773
// WITH_STDLIB

class Klass

context(_: Klass) fun foo() {
    <!CANNOT_INFER_PARAMETER_TYPE!>contextOf<!>()
    contextOf<<!CANNOT_INFER_PARAMETER_TYPE!>_<!>>()
}
