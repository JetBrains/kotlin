// FIR_IDENTICAL
// ISSUE: KT-62814

class K2DuplicatesOkayBug {
    <!CONFLICTING_OVERLOADS!>private fun startBackgroundSync()<!> {
        //todo
    }
    <!CONFLICTING_OVERLOADS!>private fun startBackgroundSync()<!> {
        //todo
    }
}
