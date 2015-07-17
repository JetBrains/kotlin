fun main(args: Array<String>) {
    val str: String? = ""

    if (str != null)
        <caret>test(str)
    else
        test("")
}

fun test(s: String) = 1