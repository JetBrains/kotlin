import java.lang.Exception

fun test(b: Boolean) {
    throw if (b) { Exception<caret>() } else { Exception() }
}