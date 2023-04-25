/*
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-37081
 */


enum class A {
    A1,
    A2,
}
class B()
class C(val b : B)
fun get(f: Boolean) = if (f) {A.A1} else {""}

<!CONFLICTING_OVERLOADS!>fun case2()<!> {

    val flag: Any = get(false) //string
    val l1 = <!NO_ELSE_IN_WHEN!>when<!> (flag<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>) { // should be NO_ELSE_IN_WHEN
        A.A1 -> B()
        A.A2 -> B()
    }

    val l2 = <!NO_ELSE_IN_WHEN!>when<!> (flag) {// should be NO_ELSE_IN_WHEN
        A.A1 -> B()
        A.A2 -> B()
    }
}

<!CONFLICTING_OVERLOADS!>fun case2()<!> {

    val flag: Any = get(true)  //A
    val l1 = <!NO_ELSE_IN_WHEN!>when<!> (flag<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>) {// should be NO_ELSE_IN_WHEN
        A.A1 -> B()
        A.A2 -> B()
    }

    val l2 = <!NO_ELSE_IN_WHEN!>when<!> (flag) {// should be NO_ELSE_IN_WHEN
        A.A1 -> B()
        A.A2 -> B()
    }
}

fun case3() {

    val flag = ""  //A
    val l1 = <!NO_ELSE_IN_WHEN!>when<!> (flag<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>) {// should be NO_ELSE_IN_WHEN
        <!INCOMPATIBLE_TYPES!>A.A1<!> -> B() //should be INCOMPATIBLE_TYPES
        <!INCOMPATIBLE_TYPES!>A.A2<!> -> B() //should be INCOMPATIBLE_TYPES
    }

    val l2 = <!NO_ELSE_IN_WHEN!>when<!> (flag) {// should be NO_ELSE_IN_WHEN
        <!INCOMPATIBLE_TYPES!>A.A1<!> -> B() //should be INCOMPATIBLE_TYPES
        <!INCOMPATIBLE_TYPES!>A.A2<!> -> B() //should be INCOMPATIBLE_TYPES
    }
}
