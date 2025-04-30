// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters, +NestedTypeAliases
// WITH_STDLIB

@MustUseReturnValue
class A(val x: String = "x") {
    constructor(a: String, b: String = "x") : this(a)

    fun test1(): String = "1"

    context(a: String)
    fun test2(): String {
        return a
    }

    fun String.test3(): String {
        return this
    }

    @IgnorableReturnValue
    fun test4(): Int = 1

    val c: String
        get() = ""

    var d: String = ""
        get() = ""
        set(value) { field = value + "" }

    context(a: String)
    val e: String
        get() = ""

    val String.f: String
        get() = ""

    lateinit var g: String

    val h: String by lazy { "" }

    typealias MyTypealias = String
}

fun usage(a: A) {
    <!RETURN_VALUE_NOT_USED!>a.test1()<!>
    <!RETURN_VALUE_NOT_USED!>a.x<!>
    <!RETURN_VALUE_NOT_USED!>A()<!>
    <!RETURN_VALUE_NOT_USED!>A("", "")<!>
    with("context") {
        <!RETURN_VALUE_NOT_USED!>a.test2()<!>
        <!RETURN_VALUE_NOT_USED!>a.e<!>
        Unit
    }
    with(a) {
        <!RETURN_VALUE_NOT_USED!>"".test3()<!>
        <!RETURN_VALUE_NOT_USED!>"".f<!>
        Unit
    }
    a.test4()
    <!RETURN_VALUE_NOT_USED!>a.c<!>
    <!RETURN_VALUE_NOT_USED!>a.d<!>
    a.d = ""
    <!RETURN_VALUE_NOT_USED!>a.g<!>
    <!RETURN_VALUE_NOT_USED!>a.h<!>
    <!UNUSED_EXPRESSION!>A.MyTypealias<!>
}