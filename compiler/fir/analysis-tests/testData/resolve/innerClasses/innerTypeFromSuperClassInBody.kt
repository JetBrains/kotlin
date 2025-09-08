// RUN_PIPELINE_TILL: BACKEND
abstract class A<X : CharSequence> {
    inner class Inner
    fun foo(x: Inner.() -> Unit) {}
}

object B : A<String>() {

    fun bar() {
        val y: Inner.() -> Unit = {}
        foo(y)
        baz(y)
    }
}

fun baz(x: (A<String>.Inner) -> Unit) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, inner, lambdaLiteral, localProperty,
objectDeclaration, propertyDeclaration, typeConstraint, typeParameter, typeWithExtension */
