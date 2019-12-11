open class SomeClass<T>
class TestSome<P> {
    object Some : SomeClass<P>() {
    }
}

class Test {
    object Some : InnerClass() {
        val a = object: InnerClass() {
        }

        fun more(): InnerClass {
            val b = InnerClass()

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
