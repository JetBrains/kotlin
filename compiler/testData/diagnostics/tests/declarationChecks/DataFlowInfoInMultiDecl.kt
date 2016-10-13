class A {
    operator fun component1() : Int = 1
    operator fun component2() : Int = 2
}

fun a(aa : A?, b : Any) {
    if (aa != null) {
        val (<!UNUSED_VARIABLE!>a1<!>, <!UNUSED_VARIABLE!>b1<!>) = <!DEBUG_INFO_SMARTCAST!>aa<!>;
    }

    if (b is A) {
        val (<!UNUSED_VARIABLE!>a1<!>, <!UNUSED_VARIABLE!>b1<!>) = <!DEBUG_INFO_SMARTCAST!>b<!>;
    }
}
