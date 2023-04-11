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
        a.x.length
    }
    if (b.x != null) {
        b.x.length
    }
    if (c.x != null) {
        c.x.length
    }
}
