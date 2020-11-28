class A {
    fun foo() {}
}

fun A.bar() {}
fun A?.buzz() {}

fun test(a : A?) {
    a.<!INAPPLICABLE_CANDIDATE!>foo<!>() // error
    a.<!INAPPLICABLE_CANDIDATE!>bar<!>() // error
    a.buzz()

    a?.foo()
    a?.bar()
    a?.buzz()
}

fun A.test2() {
    foo()
    bar()
    buzz()

    this.foo()
    this.bar()
    this.buzz()

    this?.foo() // warning
    this?.bar() // warning
    this?.buzz() // warning
}

fun A?.test3() {
    <!INAPPLICABLE_CANDIDATE!>foo<!>() // error
    <!INAPPLICABLE_CANDIDATE!>bar<!>() // error
    buzz()

    this.<!INAPPLICABLE_CANDIDATE!>foo<!>() // error
    this.<!INAPPLICABLE_CANDIDATE!>bar<!>() // error
    this.buzz()

    this?.foo()
    this?.bar()
    this?.buzz()
}
