// !WITH_NEW_INFERENCE
fun test(bal: Array<Int>) {
    var bar = 4

    val <!UNUSED_VARIABLE!>a<!>: () -> Unit = { bar += 4 }

    val <!UNUSED_VARIABLE!>b<!>: () -> Int = { <!EXPECTED_TYPE_MISMATCH!>bar = 4<!> }

    val <!UNUSED_VARIABLE!>c<!>: () -> <!UNRESOLVED_REFERENCE!>UNRESOLVED<!> = { bal[2] = 3 }

    val <!UNUSED_VARIABLE!>d<!>: () -> Int = { <!ASSIGNMENT_TYPE_MISMATCH("Int")!>bar += 4<!> }

    val <!UNUSED_VARIABLE!>e<!>: Unit = run { bar += 4 }

    val <!UNUSED_VARIABLE!>f<!>: Int = <!NI;TYPE_MISMATCH!>run { <!NI;TYPE_MISMATCH, OI;ASSIGNMENT_TYPE_MISMATCH!>bar += 4<!> }<!>
}
