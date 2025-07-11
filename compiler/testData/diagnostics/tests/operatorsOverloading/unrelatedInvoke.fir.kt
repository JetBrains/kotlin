// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76782
class Foo

class Unrelated

operator fun Unrelated.invoke() {}

fun test(foo: Foo) {
    foo(<!TOO_MANY_ARGUMENTS!>"hello world"<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, operator, stringLiteral */
