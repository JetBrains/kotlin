// WITH_RUNTIME
import java.lang.UnsupportedOperationException

val foo: String
    <caret>get() = throw UnsupportedOperationException()
