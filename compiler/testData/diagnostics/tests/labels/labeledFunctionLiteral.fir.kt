// !DIAGNOSTICS: -UNUSED_VARIABLE

val funLit = lambda@ fun String.() {
    val d1 = this<!UNRESOLVED_LABEL!>@lambda<!>
}

fun test() {
    val funLit = lambda@ fun String.(): <!UNRESOLVED_LABEL!>String<!> {
        return this<!UNRESOLVED_LABEL!>@lambda<!>
    }
}

fun lambda() {
    val funLit = lambda@ fun String.(): <!UNRESOLVED_LABEL!>String<!> {
        return this<!UNRESOLVED_LABEL!>@lambda<!>
    }
}
