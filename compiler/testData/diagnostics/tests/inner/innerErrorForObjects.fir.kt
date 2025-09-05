// RUN_PIPELINE_TILL: FRONTEND
open class SomeClass<T>
class TestSome<P> {
    object Some : SomeClass<<!UNRESOLVED_REFERENCE!>P<!>>() {
    }
}

class Test {
    object Some : <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>InnerClass<!>() {
        val a = object: <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>InnerClass<!>() {
        }

        fun more(): InnerClass {
            val b = <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>InnerClass<!>()

            val testVal = <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>inClass<!>
            <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>foo<!>()

            return b
        }
    }

    val inClass = 12
    fun foo() {
    }

    open inner class InnerClass
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, inner, integerLiteral,
localProperty, nestedClass, nullableType, objectDeclaration, propertyDeclaration, typeParameter */
