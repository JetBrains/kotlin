open class SomeClass<T>
class TestSome<P> {
    object Some : SomeClass<<!UNRESOLVED_REFERENCE!>P<!>>() {
    }
}

class Test {
    object Some : <!UNRESOLVED_REFERENCE!>InnerClass<!>() {
        val a = object: <!UNRESOLVED_REFERENCE!>InnerClass<!>() {
        }

        fun more(): InnerClass {
            val b = <!RESOLUTION_TO_CLASSIFIER!>InnerClass<!>()

            val testVal = <!UNRESOLVED_REFERENCE!>inClass<!>
            <!UNRESOLVED_REFERENCE!>foo<!>()

            return b
        }
    }

    val inClass = 12
    fun foo() {
    }

    open inner class InnerClass
}
