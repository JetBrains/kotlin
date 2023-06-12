// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-413
 * MAIN LINK: overload-resolution, resolving-callable-references, bidirectional-resolution-for-callable-calls -> paragraph 3 -> sentence 1
 * PRIMARY LINKS: overload-resolution, resolving-callable-references, bidirectional-resolution-for-callable-calls -> paragraph 3 -> sentence 2
 * overload-resolution, resolving-callable-references, bidirectional-resolution-for-callable-calls -> paragraph 3 -> sentence 3
 * SECONDARY LINKS: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 1
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 4
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 7 -> sentence 1
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 8 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: a callable reference is itself an argument to an overloaded function call
 */

// TESTCASE NUMBER: 1
class Case1() {
    companion object {
        operator fun invoke(x: CharSequence): Unit = TODO() // (3)
        operator fun invoke(x: String): String = TODO() // (4)
    }
    fun case() {
        foo(::invoke)
        foo(::invoke)
        <!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: function")!>foo(::invoke)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(::invoke)<!>
    }

    fun foo(x: (CharSequence)->Any): String = TODO() // (1.1)
    fun foo(x: (String)->Any): Unit = TODO() // (1.2)
}

// TESTCASE NUMBER: 2
class Case2() {
    companion object {
        operator fun invoke(y: Any?, x: Any?): Unit = TODO() // (1.1)
        operator fun invoke(vararg x: Int): String = TODO() // (1.2)
    }

    fun case() {
        foo(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Int, kotlin.String>")!>::invoke<!>, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Int, kotlin.String>")!>::invoke<!>)
        foo(::invoke, ::invoke)
        <!DEBUG_INFO_CALL("fqName: Case2.foo; typeCall: function")!>foo(::invoke, ::invoke)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(::invoke, ::invoke)<!>
    }

    fun foo(vararg x: (Int)->Any): String = TODO() // (1.1)
    fun foo(vararg x: (Any)->Any): Unit = TODO() // (1.2)
}


// TESTCASE NUMBER: 3
interface I {
    companion object {
        operator fun invoke(x1: ()->String, x2: Any = ""): Unit = print(1) // (1)
        operator fun invoke(y1: ()->CharSequence, y2: String = ""): String { print(2) ;return "" } // (2)
    }
}
class Case3() : I {
    companion object  {
        operator fun invoke(x: ()->CharSequence): Unit = print(3) // (3)
        operator fun invoke(x: ()->String, z: String = ""): Any { print(4); return "" } // (4)
    }

    val x = ""
    fun x() = "" as CharSequence
    val y : CharSequence= ""
    fun y() = ""

    fun case() {
        I.invoke(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KProperty0<kotlin.String>")!>::x<!>)
        I.invoke(::x)
        I.invoke(::x)

        I.invoke(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction0<kotlin.String>")!>::y<!>)
        I.invoke(::y)
        I.invoke(::y)

        I(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KProperty0<kotlin.String>")!>::x<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>I(::x)<!>
        <!DEBUG_INFO_CALL("fqName: I.Companion.invoke; typeCall: variable&invoke")!>I(::x)<!>

        I(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction0<kotlin.String>")!>::y<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>I(::y)<!>
        <!DEBUG_INFO_CALL("fqName: I.Companion.invoke; typeCall: variable&invoke")!>I(::y)<!>

        Case3(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KProperty0<kotlin.String>")!>::x<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>Case3(::x)<!>
        <!DEBUG_INFO_CALL("fqName: Case3.Companion.invoke; typeCall: variable&invoke")!>Case3(::x)<!>

        Case3(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction0<kotlin.String>")!>::y<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>Case3(::y)<!>
        <!DEBUG_INFO_CALL("fqName: Case3.Companion.invoke; typeCall: variable&invoke")!>Case3(::y)<!>
    }
}

// TESTCASE NUMBER: 4
class Case4() {
    companion object {
        operator fun invoke(x: CharSequence): Unit = TODO() // (3)
        operator fun invoke(x: String): String = TODO() // (4)
    }
    fun case(case: Case4) {
        case(::invoke)
        case(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<kotlin.CharSequence, kotlin.Unit>")!>::invoke<!>)
        <!DEBUG_INFO_CALL("fqName: Case4.invoke; typeCall: variable&invoke")!>case(::invoke)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case(::invoke)<!>
    }

    operator fun invoke(x: (CharSequence)->Any): String = TODO() // (1.1)
    operator fun invoke(x: (String)->Any): Unit = TODO() // (1.2)
}
