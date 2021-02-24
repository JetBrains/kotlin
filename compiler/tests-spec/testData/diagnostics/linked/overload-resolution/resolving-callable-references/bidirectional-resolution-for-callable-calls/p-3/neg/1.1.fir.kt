// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1() {
    companion object {
        operator fun invoke(x: CharSequence, x1: Int =1): Unit = TODO() // (3)
        operator fun invoke(x: String): String = TODO() // (4)
    }

    fun case() {
        <!NONE_APPLICABLE!>Companion<!>(<!UNRESOLVED_REFERENCE!>::x<!>)
    }

    val x = ""
    fun x() :CharSequence = ""

}

// TESTCASE NUMBER: 2
class Case2() {
    companion object {
        operator fun invoke(x: A, x1: Int =1): Unit = TODO() // (3)
        operator fun invoke(x: B): String = TODO() // (4)
    }

    fun case() {
        <!NONE_APPLICABLE!>Companion<!>(<!UNRESOLVED_REFERENCE!>::x<!>)
    }

    val x = C()
    fun x() = B()

    interface A
    class B : A
    class C : A
}
// TESTCASE NUMBER: 3
interface I {
    companion object {
        operator fun invoke(x1: ()->CharSequence, x2: Any = ""): Unit = print(1) // (1)
        operator fun invoke(y1: ()->CharSequence, y2: String = ""): String { print(2); invoke(x1 = y1, x2 = y2) ;return "" } // (2)
    }
}
class Case3() : I {
    companion object  {
        operator fun invoke(x1: ()->CharSequence, x2: Any = ""): Unit = print(3) // (3)
        operator fun invoke(y1: ()->CharSequence, y2: String = ""): Any { print(4); return "" } // (4)
    }

    val x = ""
    fun x() = "" as CharSequence

    fun case() {
        I.<!AMBIGUITY!>invoke<!>(<!UNRESOLVED_REFERENCE!>::x<!>)
        <!AMBIGUITY!>I<!>(<!UNRESOLVED_REFERENCE!>::x<!>)
        <!AMBIGUITY!>Case3<!>(<!UNRESOLVED_REFERENCE!>::x<!>)
    }
}
