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
        this.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>foo<!>()<!>
        <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>foo<!>()<!>
    }
    this.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>foo<!>()<!>
    <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>foo<!>()<!>
}

fun Any?.test_2() {
    if (this !is A) {
        this.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>foo<!>()<!>
        <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>foo<!>()<!>
    } else {
        this.foo()
        foo()
    }
    this.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>foo<!>()<!>
    <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>foo<!>()<!>
}

fun test_3(a: Any, b: Any, c: Any) {
    with(a) wa@{
        with(b) wb@{
            with(c) wc@{
                this@wb as A
                this@wb.foo()
                foo()
            }
            this.foo()
            foo()
        }
    }
}

fun Any?.test_4() {
    if (this !is A) {
        this.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>foo<!>()<!>
        <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>foo<!>()<!>
        this.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>bar<!>()<!>
        <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>bar<!>()<!>
    } else if (this !is B) {
        this.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>bar<!>()<!>
        <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>bar<!>()<!>
        this.foo()
        foo()
    } else {
        this.foo()
        foo()
        this.bar()
        bar()
    }
    this.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>foo<!>()<!>
    <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>foo<!>()<!>
    this.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>bar<!>()<!>
    <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>bar<!>()<!>
}

fun Any.test_5(): Int = when {
    this is List<*> -> size
    this is String -> length
    else -> 0
}

fun Any.test_6() {
    this as List<*>
    size
    this as String
    length
}
