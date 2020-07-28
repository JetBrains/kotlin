// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


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
        <!INAPPLICABLE_CANDIDATE!>foo<!>(<!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown"), UNRESOLVED_REFERENCE!>::invoke<!>, <!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown"), UNRESOLVED_REFERENCE!>::invoke<!>)
        <!INAPPLICABLE_CANDIDATE!>foo<!>(<!UNRESOLVED_REFERENCE!>::invoke<!>, <!UNRESOLVED_REFERENCE!>::invoke<!>)
        <!DEBUG_INFO_CALL("fqName: fqName is unknown; typeCall: unresolved")!><!INAPPLICABLE_CANDIDATE!>foo<!>(<!UNRESOLVED_REFERENCE!>::invoke<!>, <!UNRESOLVED_REFERENCE!>::invoke<!>)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Inapplicable(INAPPLICABLE): [/Case2.foo, /Case2.foo]")!><!INAPPLICABLE_CANDIDATE!>foo<!>(<!UNRESOLVED_REFERENCE!>::invoke<!>, <!UNRESOLVED_REFERENCE!>::invoke<!>)<!>
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

        <!UNRESOLVED_REFERENCE!>I<!>(<!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown"), UNRESOLVED_REFERENCE!>::x<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: I")!><!UNRESOLVED_REFERENCE!>I<!>(<!UNRESOLVED_REFERENCE!>::x<!>)<!>
        <!DEBUG_INFO_CALL("fqName: fqName is unknown; typeCall: unresolved")!><!UNRESOLVED_REFERENCE!>I<!>(<!UNRESOLVED_REFERENCE!>::x<!>)<!>

        <!UNRESOLVED_REFERENCE!>I<!>(<!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown"), UNRESOLVED_REFERENCE!>::y<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Unresolved name: I")!><!UNRESOLVED_REFERENCE!>I<!>(<!UNRESOLVED_REFERENCE!>::y<!>)<!>
        <!DEBUG_INFO_CALL("fqName: fqName is unknown; typeCall: unresolved")!><!UNRESOLVED_REFERENCE!>I<!>(<!UNRESOLVED_REFERENCE!>::y<!>)<!>

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
