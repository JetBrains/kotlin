// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
import Outer.Inner


class Outer<E> {
    inner class Inner

    fun foo() {
        class E
        val x: Inner = Inner()
    }

    class Nested {
        fun bar(x: <!OUTER_CLASS_ARGUMENTS_REQUIRED("class 'Outer'")!>Inner<!>) {}
    }
}

class E

fun bar(x: <!OUTER_CLASS_ARGUMENTS_REQUIRED("class 'Outer'")!>Inner<!>) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inner, localClass, localProperty, nestedClass,
nullableType, propertyDeclaration, typeParameter */
