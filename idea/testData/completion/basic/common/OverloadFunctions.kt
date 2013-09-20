package Test.MyTest

class A {
    class object {
        public fun testOther() {

        }

        public fun testOther(a: Boolean) {

        }

        public fun testOther(a: Int) {

        }
    }
}

fun testMy() {
    A.test<caret>
}

// EXIST: testOther
// NUMBER: 3