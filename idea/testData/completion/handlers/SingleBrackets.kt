package Test.MyTest

class A {
    default object {
        public fun testOther(a: Boolean) {

        }

        public fun testOther(a: Int) {

        }
    }
}

fun testMy() {
    A.testOther<caret>
}