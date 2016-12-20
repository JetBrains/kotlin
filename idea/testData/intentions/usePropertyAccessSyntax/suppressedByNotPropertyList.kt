// WITH_RUNTIME
// WITH_JDK
// IS_APPLICABLE: false
import java.net.Socket

fun main(args: Array<String>) {
    val s = Socket()
    val stream = s.getInputStream()<caret>
}