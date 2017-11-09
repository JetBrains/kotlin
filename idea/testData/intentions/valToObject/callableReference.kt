// IS_APPLICABLE: false

interface B {
}

val <caret>a = object : B {
}

fun foo() {
    val ref = ::a
}
