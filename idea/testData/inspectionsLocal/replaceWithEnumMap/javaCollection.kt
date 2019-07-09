// RUNTIME_WITH_FULL_JDK
import java.util.HashMap

enum class E {
    A, B
}

fun getMap(): Map<E, String> = <caret>HashMap()
