// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-77104

class Foo

class Bar
fun Bar.qux() {}

class Baz
fun Baz.qux() {}

fun test(f: Foo) {
    f.<!FUNCTION_CALL_EXPECTED, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>qux<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration */
