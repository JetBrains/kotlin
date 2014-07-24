import java.util.Collections

fun foo(a: Number) {
    val t = when (a) {
        1 -> "1"
        in Collections.singleton(2) -> "2"
        is Int -> "Int"
        !in Collections.singleton(3) -> "!3"
        !is Number -> "!Number"
        else -> null
    }
}