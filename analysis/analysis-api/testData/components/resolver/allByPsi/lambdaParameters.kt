fun foo(action1: Int.() -> Unit, action2: (Int) -> Unit) {
    val prop = 1
    prop.action1()
    action1(prop)

    prop.action2() // unresolved
    action2(prop)
}

// IGNORE_STABILITY_K1: candidates