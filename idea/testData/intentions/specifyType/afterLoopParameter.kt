// "Specify type explicitly" "true"
import java.util.HashMap

fun foo(map : HashMap<String, Int>) {
    for (entry : MutableMap.MutableEntry<String, Int><caret> in map.entrySet()) {

    }
}