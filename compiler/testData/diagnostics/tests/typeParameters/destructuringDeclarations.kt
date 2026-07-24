// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm
data class A<T>(val i: T)

fun <T> foo(block: (A<T>) -> Unit) {}

fun <T, R> bar() {
    foo<R> { [<!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>i: T<!>] ->
        i
    }
    foo<R> { (i: <!INITIALIZER_TYPE_MISMATCH!>T<!>) ->
        i
    }
}

data class C<T>(val x: Int, val y: T)

fun <T, S> foo(c: C<T>) {
    val [x: Int, y: S] = <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>c<!>
}
fun <T, S> foo2(c: C<T>) {
    val (x: Int, y: <!INITIALIZER_TYPE_MISMATCH!>S<!>) = c
}

/* GENERATED_FIR_TAGS: classDeclaration, data, destructuringDeclaration, functionDeclaration, functionalType,
lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration, typeParameter */
