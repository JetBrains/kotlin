// WITH_RUNTIME
import java.lang.UnsupportedOperationException

fun foo(): Nothing {
    <caret>throw UnsupportedOperationException()
}
