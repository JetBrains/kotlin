// ISSUE: KT-47490

fun test() {
    "1234".apply {
        try {
        } finally {
            ::<!UNRESOLVED_REFERENCE!>fu1<!>
        }
    }
}
