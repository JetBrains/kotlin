// WITH_RUNTIME
// IS_APPLICABLE: false
import java.util.Collections

val foo = Collections.unmodifiableList(listOf(1)).toMutableList()<caret>