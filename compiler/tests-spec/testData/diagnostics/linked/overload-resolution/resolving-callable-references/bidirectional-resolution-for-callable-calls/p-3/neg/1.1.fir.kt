// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-413
 * MAIN LINK: overload-resolution, resolving-callable-references, bidirectional-resolution-for-callable-calls -> paragraph 3 -> sentence 1
 * PRIMARY LINKS: overload-resolution, resolving-callable-references, bidirectional-resolution-for-callable-calls -> paragraph 3 -> sentence 2
 * overload-resolution, resolving-callable-references, bidirectional-resolution-for-callable-calls -> paragraph 3 -> sentence 3
 * overload-resolution, resolving-callable-references, resolving-callable-references-not-used-as-arguments-to-a-call -> paragraph 5 -> sentence 1
 * SECONDARY LINKS: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 1
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 4
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 7 -> sentence 1
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 8 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: a callable reference is itself an argument to an overloaded function call
 */

// TESTCASE NUMBER: 1
class Case1() {
    companion object {
        operator fun invoke(x: CharSequence, x1: Int =1): Unit = TODO() // (3)
        operator fun invoke(x: String): String = TODO() // (4)
    }

    fun case() {
        Companion(::<!NONE_APPLICABLE!>x<!>)
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
        Companion(::<!NONE_APPLICABLE!>x<!>)
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
        I.<!OVERLOAD_RESOLUTION_AMBIGUITY!>invoke<!>(::<!OVERLOAD_RESOLUTION_AMBIGUITY!>x<!>)
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>I<!>(::<!OVERLOAD_RESOLUTION_AMBIGUITY!>x<!>)
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>Case3<!>(::<!OVERLOAD_RESOLUTION_AMBIGUITY!>x<!>)
    }
}
