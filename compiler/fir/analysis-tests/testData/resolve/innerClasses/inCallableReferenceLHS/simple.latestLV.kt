// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66344
// LATEST_LV_DIFFERENCE

abstract class Outer<X> {
    inner class Inner(val x: X) {
        fun foo() {}
        val bar: Int = 42

        inner class Innermost {
            fun foo() {
                val refX: Inner.() -> X = Inner::x
                val kRefX = Inner::x
            }
        }
    }

    val refFoo: Inner.() -> Unit = Inner::foo
    val refBar: Inner.() -> Int = Inner::bar
    val refX: Inner.() -> X = Inner::x
    val kRefFoo = Inner::foo
    val kRefBar = Inner::bar
    val kRefX = Inner::x

    class Nested {
        val refX: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>.() -> <!UNRESOLVED_REFERENCE!>X<!> = <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>::x
        val kRefX = <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>::x
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, functionalType, inner, integerLiteral,
nullableType, objectDeclaration, propertyDeclaration, starProjection, typeParameter, typeWithExtension */
