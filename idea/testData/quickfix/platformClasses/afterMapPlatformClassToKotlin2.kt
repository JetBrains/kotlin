// "Change 'java.lang.Comparable<T>' to 'Comparable<T>'" "true"
import java.lang.*;

fun <T> foo() : Comparable<T>? {
    return null
}
