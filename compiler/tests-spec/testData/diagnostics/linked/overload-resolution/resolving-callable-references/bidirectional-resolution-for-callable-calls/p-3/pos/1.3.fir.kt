// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1() {
    fun foo(x: () -> Int): String = TODO() // (1.1)
    fun foo(x: () -> Any): Unit = TODO() // (1.2)

    fun case1() {
        foo(::x)
        <!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: function")!>foo(::x)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(::x)<!>
    }
}

val x = 1
fun x() = 1.0

fun case1(case: Case1) {
    case.foo(::x)
    case.<!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: function")!>foo(::x)<!>
    case.foo(::x)
}

// TESTCASE NUMBER: 2
class Case2() {
    fun foo(vararg x: ()->Short): String = TODO() // (1.1)
    fun foo(vararg x: ()->Byte): Unit = TODO() // (1.2)

    val x : Short = 1
    fun x() = 1

    val y = 1
    fun y(): Short = 1

    fun case2(case: Case2) {
        case.foo(::x, ::x)
        case.<!DEBUG_INFO_CALL("fqName: Case2.foo; typeCall: function")!>foo(::x, ::x)<!>
        case.foo(::x, ::x)

        this.foo(::x, ::x)
        this.<!DEBUG_INFO_CALL("fqName: Case2.foo; typeCall: function")!>foo(::x, ::x)<!>
        this.foo(::x, ::x)

        //for y
        case.foo(::y, ::y)
        case.<!DEBUG_INFO_CALL("fqName: Case2.foo; typeCall: function")!>foo(::y, ::y)<!>
        case.foo(::y, ::y)

        this.foo(::x, ::y)
        this.<!DEBUG_INFO_CALL("fqName: Case2.foo; typeCall: function")!>foo(::x, ::y)<!>
        this.foo(::x, ::y)
    }

}


// TESTCASE NUMBER: 3
class Case3() {
    fun foo(vararg x: ()->Short): String = TODO() // (1.1)
    fun foo(x: ()->Byte): Unit = TODO() // (1.2)

    val x : Short = 1
    fun x() = 1

    val y = 1
    fun y(): Short = 1

    fun case3(case: Case3) {
        case.foo(::x)
        case.<!DEBUG_INFO_CALL("fqName: Case3.foo; typeCall: function")!>foo(::x)<!>
        case.foo(::x)

        this.foo(::x)
        this.<!DEBUG_INFO_CALL("fqName: Case3.foo; typeCall: function")!>foo(::x)<!>
        this.foo(::x)

        //for y
        case.foo(::y)
        case.<!DEBUG_INFO_CALL("fqName: Case3.foo; typeCall: function")!>foo(::y)<!>
        case.foo(::y)

        this.foo(::x, ::y)
        this.<!DEBUG_INFO_CALL("fqName: Case3.foo; typeCall: function")!>foo(::x, ::y)<!>
        this.foo(::x, ::y)
    }
}



// TESTCASE NUMBER: 4
class Case4() {
    infix fun foo(x: ()->Int): String = TODO() // (1.1)
    infix fun foo(x: ()->Any): Unit = TODO() // (1.2)

    val x = 1
    fun x() = 1.0

    fun case() {
        <!DEBUG_INFO_CALL("fqName: Case4.foo; typeCall: infix function")!>this foo ::x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>this foo ::x<!>
        <!DEBUG_INFO_CALL("fqName: Case4.foo; typeCall: infix function")!>foo(::x)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(::x)<!>
        this.<!DEBUG_INFO_CALL("fqName: Case4.foo; typeCall: infix function")!>foo(::x)<!>
        this.foo(::x)
    }
}

fun case4(case: Case4) {
    <!DEBUG_INFO_CALL("fqName: Case4.foo; typeCall: infix function")!>case foo ::x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case foo ::x<!>
    case.<!DEBUG_INFO_CALL("fqName: Case4.foo; typeCall: infix function")!>foo(::x)<!>
    case.foo(::x)
}
