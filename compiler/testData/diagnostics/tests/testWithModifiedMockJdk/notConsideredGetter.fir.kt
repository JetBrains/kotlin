// JDK_KIND: MODIFIED_MOCK_JDK

fun foo(jalw: java.util.ListWithSomethingOverridden<String>, jal: java.util.ArrayList<String>, l: List<String>) {
    // java.util.ListWithSomethingOverridden contains getSomethingNonExisting() explicit override
    jalw.<!DEPRECATION!>somethingNonExisting<!>
    jalw.<!DEPRECATION!>getSomethingNonExisting<!>()
    // java.util.ArrayList does not contain explicit override
    jal.<!UNRESOLVED_REFERENCE!>somethingNonExisting<!>
    jal.<!UNRESOLVED_REFERENCE!>getSomethingNonExisting<!>()
    // Modified java.util.List contains additional getSomethingNonExisting(): String declaration
    l.<!UNRESOLVED_REFERENCE!>somethingNonExisting<!>
    l.<!UNRESOLVED_REFERENCE!>getSomethingNonExisting<!>()
}
