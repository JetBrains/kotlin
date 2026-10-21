// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89593

class Context<T>

class C {
    companion object {
        context(_: Context<T>)
        val <T> generic: C
            get() = C()

        context(_: Context<String>)
        val simple: C
            get() = C()

        val noContext: C
            get() = C()
    }
}

fun foo(p: C) {}

context(_: Context<String>)
fun testWithContext() {
    foo(<!UNRESOLVED_REFERENCE!>generic<!>)
    foo(<!UNRESOLVED_REFERENCE!>simple<!>)
    foo(noContext)
    val c: C = <!UNRESOLVED_REFERENCE!>simple<!>
}

fun testWithoutContext() {
    foo(<!UNRESOLVED_REFERENCE!>generic<!>)
    foo(<!UNRESOLVED_REFERENCE!>simple<!>)
    foo(noContext)
}

sealed class G<T> {
    object A : G<String>()

    companion object {
        context(_: String)
        val a: G<String> get() = A

        val b: G<String> get() = A
    }
}

class Box<T> {
    fun put(g: G<T>) = Unit
}

fun <T> buildBox(b: Box<T>.() -> Unit) = Unit

context(_: String)
fun testPostponed() {
    <!CANNOT_INFER_PARAMETER_TYPE!>buildBox<!> {
        put(<!UNRESOLVED_REFERENCE!>a<!>)
    }

    buildBox {
        put(b)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, functionDeclarationWithContext,
functionalType, getter, lambdaLiteral, localProperty, nestedClass, nullableType, objectDeclaration, propertyDeclaration,
propertyDeclarationWithContext, sealed, typeParameter, typeWithExtension */
