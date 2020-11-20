// !DIAGNOSTICS: -UNUSED_VARIABLE

val funLit = lambda@ fun String.() {
    val d1 = <!UNRESOLVED_LABEL!>this@lambda<!>
}

fun test() {
    val funLit = lambda@ fun String.(): String {
        return <!UNRESOLVED_LABEL!>this@lambda<!>
    }
}

fun lambda() {
    val funLit = lambda@ fun String.(): String {
        return <!UNRESOLVED_LABEL!>this@lambda<!>
    }
}