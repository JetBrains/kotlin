class A {
    fun foo() {}
}

fun A.bar() {}
fun A?.buzz() {}

fun test(a : A?) {
    a<!UNSAFE_CALL!>.<!>foo() // error
    a<!UNSAFE_CALL!>.<!>bar() // error
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

    this<!UNNECESSARY_SAFE_CALL!>?.<!>foo() // warning
    this<!UNNECESSARY_SAFE_CALL!>?.<!>bar() // warning
    this<!UNNECESSARY_SAFE_CALL!>?.<!>buzz() // warning
}

fun A?.test3() {
    <!UNSAFE_CALL!>foo<!>() // error
    <!UNSAFE_CALL!>bar<!>() // error
    buzz()

    this<!UNSAFE_CALL!>.<!>foo() // error
    this<!UNSAFE_CALL!>.<!>bar() // error
    this.buzz()

    this?.foo()
    this?.bar()
    this?.buzz()
}
