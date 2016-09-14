// WITH_RUNTIME
import java.lang.UnsupportedOperationException

fun foo(): Unit {
    <caret>throw UnsupportedOperationException()
}
