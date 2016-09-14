// WITH_RUNTIME
import java.lang.UnsupportedOperationException

fun foo() {
    <caret>throw UnsupportedOperationException()
}
