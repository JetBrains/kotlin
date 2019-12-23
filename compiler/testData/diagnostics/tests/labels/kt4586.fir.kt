// !DIAGNOSTICS: -UNUSED_VARIABLE
//KT-4586 this@ does not work for builders

fun string(init: StringBuilder.() -> Unit): String{
    val answer = StringBuilder()
    answer.init()
    return answer.toString()
}

val str = string l@{
    append("hello, ")

    val sub = string {
        append("world!")
        this@l.append(this)
    }
}
