// "Change 'java.lang.Comparable<T>' to 'Comparable<T>'" "true"
import java.lang.*;

fun <T> foo() : java.lang.Comparable<T><caret>? {
    return null
}
