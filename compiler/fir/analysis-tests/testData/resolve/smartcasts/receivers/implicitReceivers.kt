// !DUMP_CFG
class A {
    fun foo() {}
}

class B {
    fun bar() {}
}

fun <T> T.with(block: T.() -> Unit) {}

fun Any?.test_1() {
    if (this is A) {
        this.foo()
        foo()
    } else {
        this.<!UNRESOLVED_REFERENCE!>foo<!>()
        <!UNRESOLVED_REFERENCE!>foo<!>()
    }
    this.<!UNRESOLVED_REFERENCE!>foo<!>()
    <!UNRESOLVED_REFERENCE!>foo<!>()
}

fun Any?.test_2() {
    if (this !is A) {
        this.<!UNRESOLVED_REFERENCE!>foo<!>()
        <!UNRESOLVED_REFERENCE!>foo<!>()
    } else {
        this.foo()
        foo()
    }
    this.<!UNRESOLVED_REFERENCE!>foo<!>()
    <!UNRESOLVED_REFERENCE!>foo<!>()
}

fun test_3(a: Any, b: Any, c: Any) {
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>(a) wa@<!CANNOT_INFER_PARAMETER_TYPE!>{
        <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>(b) wb@<!CANNOT_INFER_PARAMETER_TYPE!>{
            with(c) wc@{
                this@wb as A
                this@wb.foo()
                foo()
            }
            this.<!UNRESOLVED_REFERENCE!>foo<!>()
            <!UNRESOLVED_REFERENCE!>foo<!>()
        }<!>
    }<!>
}

fun Any?.test_4() {
    if (this !is A) {
        this.<!UNRESOLVED_REFERENCE!>foo<!>()
        <!UNRESOLVED_REFERENCE!>foo<!>()
        this.<!UNRESOLVED_REFERENCE!>bar<!>()
        <!UNRESOLVED_REFERENCE!>bar<!>()
    } else if (<!USELESS_IS_CHECK!>this !is B<!>) {
        this.<!UNRESOLVED_REFERENCE!>bar<!>()
        <!UNRESOLVED_REFERENCE!>bar<!>()
        this.foo()
        foo()
    } else {
        this.foo()
        foo()
        this.bar()
        bar()
    }
    this.<!UNRESOLVED_REFERENCE!>foo<!>()
    <!UNRESOLVED_REFERENCE!>foo<!>()
    this.<!UNRESOLVED_REFERENCE!>bar<!>()
    <!UNRESOLVED_REFERENCE!>bar<!>()
}

fun Any.test_5(): Int = when {
    this is List<*> -> size
    this is String -> length
    else -> 0
}

fun Any.test_6() {
    this as List<*>
    size
    this <!CAST_NEVER_SUCCEEDS!>as<!> String
    length
}
