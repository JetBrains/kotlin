// WITH_RUNTIME
import java.lang.System.out

fun x() {
    listOf("")
        .take(10)
        .forEach { out.<caret>print(it) }
}