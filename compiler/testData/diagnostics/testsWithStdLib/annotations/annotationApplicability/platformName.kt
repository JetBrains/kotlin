// !DIAGNOSTICS: -UNUSED_VARIABLE

import kotlin.platform.*

[platformName("a")]
fun foo() {}

[platformName("b")]
fun Any.foo() {}

[<!INAPPLICABLE_ANNOTATION!>platformName("c")<!>]
val px = 1

[<!INAPPLICABLE_ANNOTATION!>platformName("d")<!>]
val Any.px : Int
    get() = 1

val valx: Int
    [platformName("e")]
    get() = 1

var varx: Int
    [platformName("f")]
    get() = 1
    [platformName("g")]
    set(v) {}

var vardef: Int = 1
    [platformName("f")]
    get
    [platformName("g")]
    set

[<!INAPPLICABLE_ANNOTATION!>platformName("C")<!>]
class C {
    [<!INAPPLICABLE_ANNOTATION!>platformName("a")<!>]
    fun foo() {}

    [<!INAPPLICABLE_ANNOTATION!>platformName("b")<!>]
    fun Any.foo() {}

    [<!INAPPLICABLE_ANNOTATION!>platformName("c")<!>]
    val px = 1

    [<!INAPPLICABLE_ANNOTATION!>platformName("d")<!>]
    val Any.px : Int
        get() = 1

    val valx: Int
        [<!INAPPLICABLE_ANNOTATION!>platformName("e")<!>]
        get() = 1

    var varx: Int
        [<!INAPPLICABLE_ANNOTATION!>platformName("f")<!>]
        get() = 1
        [<!INAPPLICABLE_ANNOTATION!>platformName("g")<!>]
        set(v) {}
}

fun foo1() {
    [<!INAPPLICABLE_ANNOTATION!>platformName("a")<!>]
    fun foo() {}

    [<!INAPPLICABLE_ANNOTATION!>platformName("a")<!>]
    val x = 1
}