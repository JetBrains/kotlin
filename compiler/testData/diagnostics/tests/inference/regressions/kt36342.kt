import java.lang.Exception

fun <K> id(arg: K): K = arg

fun test() {
    id(<!UNRESOLVED_REFERENCE!>unresolved<!>)<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    <!UNRESOLVED_REFERENCE!>unresolved<!>!!<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    try {
        id(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    } catch (e: Exception) {
        id(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    }

    if (true)
        id(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    else
        id(<!UNRESOLVED_REFERENCE!>unresolved<!>)

    when {
        true -> id(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    }
    id(<!UNRESOLVED_REFERENCE!>unresolved<!>) ?: id(<!UNRESOLVED_REFERENCE!>unresolved<!>)
}
