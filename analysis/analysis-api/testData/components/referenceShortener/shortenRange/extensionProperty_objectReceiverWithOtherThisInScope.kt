package test

val <T> T.extProperty: T
    get() = this

object Bar

class Other

fun Other.usage() {
    <expr>Bar.extProperty</expr>
}
