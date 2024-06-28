// ISSUE: KT-57893
// MODULE: main
internal class A {
    val x: String? = null
}

class B {
    internal val x: String? = null
}

class C {
    val x: String? = null
}

// MODULE: test()(main)
internal fun test(a: A, b: B, c: C) {
    if (a.x != null) {
        <!DEBUG_INFO_SMARTCAST!>a.x<!>.length
    }
    if (b.x != null) {
        <!DEBUG_INFO_SMARTCAST!>b.x<!>.length
    }
    if (c.x != null) {
        <!SMARTCAST_IMPOSSIBLE!>c.x<!>.length
    }
}
