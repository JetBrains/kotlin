package test

fun <T> T.extFun(): T = this

object Bar

class Other

fun Other.usage() {
    <expr>Bar.extFun()</expr>
}
