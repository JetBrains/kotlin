// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82122

class Outer<A> {
    inner class Middle<B> {
        inner class Inner<C>
    }
}

fun Outer<Int>.Middle<String>.Inner<Char>.foo() {
}

fun test() {
    Outer<Int>.Middle<String>.Inner<Char>::foo
    Outer<Int>.Middle.Inner<Char, String>::foo
    Outer.Middle.Inner<Char, String, Int>::foo
    Outer<Char, String, Int>.Middle.Inner::foo
    Outer.Middle<Int>.Inner<Char, String>::foo
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration, inner,
nullableType, typeParameter */
