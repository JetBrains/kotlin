// ISSUE: KT-65235

class Scope {
    operator fun String.invoke(): Int = 1
}

fun scope(s: Scope.() -> Unit) {}

fun somewhere() {
    val x: String = listOf("a").first()

    scope {
        val y: String = <!TYPE_MISMATCH!>listOf("a").<!TYPE_MISMATCH!><!DEPRECATION_ERROR!>first<!>()<!><!>
    }
}
