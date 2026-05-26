// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-61182

object Foo

operator fun Foo.invoke(f: () -> Unit) {
    f()
}

fun test(g: () -> Int) {
    Foo(g)
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, objectDeclaration, operator */
