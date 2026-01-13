// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66344
// LATEST_LV_DIFFERENCE

abstract class Outer<X> {
    inner class Inner(val x: X) {
        fun foo() {}
        val bar: Int = 42

        inner class Innermost {
            fun foo() {
                val refX: Inner.() -> X <!INITIALIZER_TYPE_MISMATCH!>=<!> <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::x
                val kRefX = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::x
            }
        }
    }

    val refFoo: Inner.() -> Unit = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::foo
    val refBar: Inner.() -> Int = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::bar
    val refX: Inner.() -> X <!INITIALIZER_TYPE_MISMATCH!>=<!> <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::x
    val kRefFoo = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::foo
    val kRefBar = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::bar
    val kRefX = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::x

    class Nested {
        val refX: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>.() -> <!UNRESOLVED_REFERENCE!>X<!> = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::x
        val kRefX = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>::x
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, functionalType, inner, integerLiteral,
nullableType, objectDeclaration, propertyDeclaration, starProjection, typeParameter, typeWithExtension */
