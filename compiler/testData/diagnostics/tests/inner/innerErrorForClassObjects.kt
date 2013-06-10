open class SomeClass<T>
class TestSome<P> {
    class object : SomeClass<<!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>P<!>>() {
    }
}

class Test {
    class object : <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>InnerClass()<!> {
        val a = object: <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>InnerClass()<!> {
        }

        fun more(): InnerClass {
            val b = <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>InnerClass()<!>

            val <!UNUSED_VARIABLE!>testVal<!> = <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>inClass<!>
            <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>foo()<!>

            return b
        }
    }

    val inClass = 12
    fun foo() {}

    open inner class InnerClass
}