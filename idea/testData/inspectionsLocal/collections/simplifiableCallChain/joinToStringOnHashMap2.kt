// PROBLEM: none
// WITH_RUNTIME
import java.util.*

class MyMapClass: HashMap<String, String>()

fun test(data: MyMapClass) {
    val result = data.<caret>map { "${it.key}: ${it.value}" }.joinToString("\n")
}