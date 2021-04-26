// !DIAGNOSTICS: -UNUSED_VARIABLE

val funLit = lambda@ fun String.() {
    val d1 = <!UNRESOLVED_LABEL!>this@lambda<!>
}

fun test() {
    val funLit = lambda@ fun String.(): <!UNRESOLVED_LABEL!>String<!> {
        return <!UNRESOLVED_LABEL!>this@lambda<!>
    }
}

fun lambda() {
    val funLit = lambda@ fun String.(): <!UNRESOLVED_LABEL!>String<!> {
        return <!UNRESOLVED_LABEL!>this@lambda<!>
    }
}