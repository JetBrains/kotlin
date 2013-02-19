// "Change 'java.lang.Iterable<T>' to a Kotlin class" "true"
import java.lang.*;

fun <T> foo() : java.lang.Iterable<T><caret>? {
    return null
}
