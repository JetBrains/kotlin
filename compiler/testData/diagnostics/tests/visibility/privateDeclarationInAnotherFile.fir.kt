// FILE: 1.kt

private class Private {
    class Public
}

// FILE: 2.kt

import <!INVISIBLE_REFERENCE!>Private<!>.Public

private fun test_1(x: <!INVISIBLE_REFERENCE!>Private.Public<!>, y: <!INVISIBLE_REFERENCE!>Public<!>) {

}
