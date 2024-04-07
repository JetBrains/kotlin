// !DIAGNOSTICS: -REDUNDANT_LABEL_WARNING

fun foo1() {
    l2@ l1@ do {
    } while (true)
}

fun foo2() {
    l4@ <!UNDERSCORE_IS_RESERVED!>_<!>@ l3@ <!UNDERSCORE_IS_RESERVED!>__<!>@ l2@ l1@ while (true) {
    }
}

fun foo3() {
    l2@ l1@ 42
}

fun foo4() {
    l1@ do {
        l4@ { false }
    } while (true)
}

fun foo5() {
    l2@ l1@ do {
        l4@ { false }
    } while (true)
}

fun foo6() {
    l2@ l1@ do {
        l4@ l3@{ true }
    } while (true)
}

fun foo7() {
    l3@ l2@ l1@ fun bar() {}
}

fun func(y: (Unit) -> Unit) {}

fun foo8() {
    func(l@ {})
    func(l2@ l1@ {})
    func(<!UNDERSCORE_IS_RESERVED!>_<!>@ {})
}
