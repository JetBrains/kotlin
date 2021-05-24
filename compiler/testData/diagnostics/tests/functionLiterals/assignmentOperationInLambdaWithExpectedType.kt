fun test(bal: Array<Int>) {
    var bar = 4

    val a: () -> Unit = { bar += 4 }

    val b: () -> Int = { <!EXPECTED_TYPE_MISMATCH!>bar = 4<!> }

    val c: () -> <!UNRESOLVED_REFERENCE!>UNRESOLVED<!> = { bal[2] = 3 }

    val d: () -> Int = { <!ASSIGNMENT_TYPE_MISMATCH("Int")!>bar += 4<!> }

    val e: Unit = run { bar += 4 }

    val f: Int = <!TYPE_MISMATCH!>run { <!TYPE_MISMATCH!>bar += 4<!> }<!>
}
