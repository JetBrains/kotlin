// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

context(s: S) fun <S> test2(element: S) { }
context(s: String) fun test2(element: String) { }

context(s: String)
fun example2() {
    test2("")
}

interface Component<S>

context(component: Component<S>) fun <S> test1(element: Int?) { }
context(component: Component<S>) fun <S> test1(vararg elements: Int?) { }

context(component: Component<Int>)
fun example1() {
    test1()
    test1(1)
    test1(null)
    test1(1, 2)
    test1(1, null)
    test1(<!ARGUMENT_TYPE_MISMATCH!>true<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, integerLiteral, interfaceDeclaration,
nullableType, typeParameter, vararg */
