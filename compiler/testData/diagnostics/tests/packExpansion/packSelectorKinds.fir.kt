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

fun TypeAttrs(...ButtonProps.$attrs) {
    text.length
    enabled.not()
    <!UNRESOLVED_REFERENCE!>onClick<!>
    <!UNRESOLVED_REFERENCE!>content<!>
}

fun TypeCallbacks(...ButtonProps.$callbacks) {
    onClick()
    <!UNRESOLVED_REFERENCE!>text<!>
    <!UNRESOLVED_REFERENCE!>content<!>
}

fun TypeSlots(...ButtonProps.$slots) {
    content()
    <!UNRESOLVED_REFERENCE!>text<!>
    <!UNRESOLVED_REFERENCE!>onClick<!>
}

fun FunctionAttrsAmbiguous(<!OVERLOAD_RESOLUTION_AMBIGUITY!>...Button.$attrs<!>) {}

fun FunctionAttrs(...Button.$attrs(textButton)) {
    text.length
    enabled.not()
    <!UNRESOLVED_REFERENCE!>onClick<!>
    <!UNRESOLVED_REFERENCE!>content<!>
    <!UNRESOLVED_REFERENCE!>value<!>
}

fun FunctionCallbacks(...Button.$callbacks(textButton)) {
    onClick()
    <!UNRESOLVED_REFERENCE!>text<!>
    <!UNRESOLVED_REFERENCE!>content<!>
}

fun FunctionSlots(...Button.$slots(textButton)) {
    content()
    <!UNRESOLVED_REFERENCE!>text<!>
    <!UNRESOLVED_REFERENCE!>onClick<!>
}

fun ValueAttrs(...textButton.$attrs) {
    text.length
    enabled.not()
    <!UNRESOLVED_REFERENCE!>onClick<!>
    <!UNRESOLVED_REFERENCE!>content<!>
}

fun ValueCallbacks(...textButton.$callbacks) {
    onClick()
    <!UNRESOLVED_REFERENCE!>text<!>
    <!UNRESOLVED_REFERENCE!>content<!>
}

fun ValueSlots(...textButton.$slots) {
    content()
    <!UNRESOLVED_REFERENCE!>text<!>
    <!UNRESOLVED_REFERENCE!>onClick<!>
}
