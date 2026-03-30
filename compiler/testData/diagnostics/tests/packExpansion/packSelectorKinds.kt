// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

@Target(AnnotationTarget.TYPE)
annotation class Composable

class ButtonProps {
    val text: String = ""
    val enabled: Boolean = false
    val onClick: () -> String = { "" }
    val content: @Composable () -> String = { "" }
}

fun Button(text: String, enabled: Boolean, onClick: () -> String, content: @Composable () -> String) {}

fun Button(value: Int, enabled: Boolean, content: @Composable () -> String) {}

val textButton: (text: String, enabled: Boolean, onClick: () -> String, content: @Composable () -> String) -> Unit = ::Button

fun TypeAttrs(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>ButtonProps<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$attrs<!><!>) {
    <!UNRESOLVED_REFERENCE!>text<!>
    <!UNRESOLVED_REFERENCE!>enabled<!>
    <!UNRESOLVED_REFERENCE!>onClick<!>
    <!UNRESOLVED_REFERENCE!>content<!>
}

fun TypeCallbacks(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>ButtonProps<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$callbacks<!><!>) {
    <!UNRESOLVED_REFERENCE!>onClick<!>
}

fun TypeSlots(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>ButtonProps<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$slots<!><!>) {
    <!UNRESOLVED_REFERENCE!>content<!>
}

fun FunctionAttrsAmbiguous(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>Button<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$attrs<!><!>) {}

fun FunctionAttrs(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>Button<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$attrs<!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>textButton<!>)<!>) {
    <!UNRESOLVED_REFERENCE!>text<!>
    <!UNRESOLVED_REFERENCE!>enabled<!>
}

fun FunctionCallbacks(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>Button<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$callbacks<!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>textButton<!>)<!>) {
    <!UNRESOLVED_REFERENCE!>onClick<!>
}

fun FunctionSlots(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>Button<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$slots<!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>textButton<!>)<!>) {
    <!UNRESOLVED_REFERENCE!>content<!>
}

fun ValueAttrs(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>textButton<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$attrs<!><!>) {
    <!UNRESOLVED_REFERENCE!>text<!>
    <!UNRESOLVED_REFERENCE!>enabled<!>
}

fun ValueCallbacks(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>textButton<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$callbacks<!><!>) {
    <!UNRESOLVED_REFERENCE!>onClick<!>
}

fun ValueSlots(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>textButton<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$slots<!><!>) {
    <!UNRESOLVED_REFERENCE!>content<!>
}
