// ISSUE: KT-62814

class K2DuplicatesOkayBug {
    private <!CONFLICTING_OVERLOADS!>fun startBackgroundSync()<!> {
        //todo
    }
    private <!CONFLICTING_OVERLOADS!>fun startBackgroundSync()<!> {
        //todo
    }
}
