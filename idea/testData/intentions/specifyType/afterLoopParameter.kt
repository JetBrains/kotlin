// "Specify Type Explicitly" "true"
import java.util.HashMap
import java.util.Map

fun foo(map : HashMap<String, Int>) {
    for (<caret>entry : Map.Entry<String, Int> in map.entrySet()) {

    }
}