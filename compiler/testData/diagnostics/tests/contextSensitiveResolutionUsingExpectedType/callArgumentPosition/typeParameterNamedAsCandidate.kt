// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89822

sealed class Parent {
    class T : Parent()
    object U : Parent()

    companion object {
        val V: Parent = U
    }
}

fun foo(a: Parent) {}

inline fun <reified T, reified U, reified V> reified() {
    foo(<!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>)
    foo(<!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>U<!>)
    foo(<!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>V<!>)
}

fun <T, U, V> plain() {
    foo(<!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>)
    foo(<!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>U<!>)
    foo(<!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>V<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, inline, nestedClass, nullableType,
objectDeclaration, propertyDeclaration, reified, sealed, typeParameter */
