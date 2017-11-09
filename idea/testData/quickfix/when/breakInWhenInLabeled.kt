// "Add '@loop' to break" "true"
// WITH_RUNTIME

fun foo(chars: CharArray) {
    val length = chars.size
    var pos = 0
    loop@ while (pos < length) {
        val c = chars[pos]
        when (c) {
            '\n' -> br<caret>eak
        }
        pos++
    }
}