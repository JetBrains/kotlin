// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-62814

class K2DuplicatesOkayBug {
    <!CONFLICTING_OVERLOADS!>private fun startBackgroundSync()<!> {
        //todo
    }
    <!CONFLICTING_OVERLOADS!>private fun startBackgroundSync()<!> {
        //todo
    }
}
