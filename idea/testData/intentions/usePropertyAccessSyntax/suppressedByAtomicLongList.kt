// WITH_RUNTIME
// WITH_JDK
// IS_APPLICABLE: false
import java.util.concurrent.atomic.AtomicLong

fun main() {
    val l = AtomicLong()
    val x = l.get<caret>AndIncrement()
}