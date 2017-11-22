// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_EXPRESSION

import java.util.HashSet

fun test123() {
    val g: (Int) -> Unit = if (true) {
        val set = <!NI;DEBUG_INFO_MISSING_UNRESOLVED!>HashSet<!><<!NI;DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>>();
        { i ->
            <!NI;UNRESOLVED_REFERENCE!>set<!>.<!NI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>add<!>(i)
        }
    }
    else {
        { it -> it }
    }
}