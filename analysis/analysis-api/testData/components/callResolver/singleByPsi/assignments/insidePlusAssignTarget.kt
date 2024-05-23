// WITH_STDLIB

fun foo(x: MutableMap<Int, MutableList<String>>) {
    x.getOrPut(1) { <expr>mutableListOf</expr><String>() } += "str"
}