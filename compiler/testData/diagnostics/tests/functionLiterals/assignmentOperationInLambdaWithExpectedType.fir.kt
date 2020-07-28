// !WITH_NEW_INFERENCE
fun test(bal: Array<Int>) {
    var bar = 4

    val a: () -> Unit = { bar += 4 }

    val b: () -> Int = { bar = 4 }

    val c: () -> <!UNRESOLVED_REFERENCE!>UNRESOLVED<!> = { bal[2] = 3 }

    val d: () -> Int = { bar += 4 }

    val e: Unit = run { bar += 4 }

    val f: Int = run { bar += 4 }
}
