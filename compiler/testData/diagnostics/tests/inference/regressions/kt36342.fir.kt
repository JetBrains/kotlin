
import java.lang.Exception

fun <K> id(arg: K): K = arg

fun test() {
    id(<!UNRESOLVED_REFERENCE!>unresolved<!>)!!
    <!UNRESOLVED_REFERENCE!>unresolved<!>!!!!
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
        true -> <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    }
    id(<!UNRESOLVED_REFERENCE!>unresolved<!>) ?: id(<!UNRESOLVED_REFERENCE!>unresolved<!>)
}
