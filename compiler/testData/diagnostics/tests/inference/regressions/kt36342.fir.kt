
import java.lang.Exception

fun <K> id(arg: K): K = arg

fun test() {
    id(<!UNRESOLVED_REFERENCE!>unresolved<!>)!!
    <!UNRESOLVED_REFERENCE!>unresolved<!>!!<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>try {
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    } catch (e: Exception) {
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    }<!>

    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>if (true)
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    else
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)<!>

    when {
        true -> id(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    }
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!><!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>) ?: <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)<!>
}
