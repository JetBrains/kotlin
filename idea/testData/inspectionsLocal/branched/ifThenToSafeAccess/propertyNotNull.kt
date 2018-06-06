
// FIX: Remove redundant 'if' expression

interface Bar

data class Data(val bar: Bar?)

fun handle(data: Data) {
    val bar = <caret>if (data.bar != null) data.bar else null
}