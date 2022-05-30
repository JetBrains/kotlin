class Outer {
    val i = Inner()
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val tag = " "<!>

    inner class Inner {
        val innerTag = tag
    }
}

class Outer1(outer1: Outer1) {
    val i = Inner()
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val b: String = i.a.b<!>

    inner class Inner {
        val a: Outer1 = this@Outer1

//        constructor(outer1: Outer1) {
//            a = outer1
//        }
//
//        constructor() {
//            a = this@Outer1
//        }
    }
}
