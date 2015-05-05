// WITH_RUNTIME

import java.util.HashMap

fun foo(map : HashMap<String, Int>) {
    for (<caret>entry in map.entrySet()) {

    }
}