// KJS_WITH_FULL_RUNTIME
package foo

fun box(): String {
    val s = StringBuilder()
    s.append("a")
    s.append("b").append("c")
    s.append('d').append("e")

    if (s.toString() != "abcde") return s.toString()
    return "OK"
}