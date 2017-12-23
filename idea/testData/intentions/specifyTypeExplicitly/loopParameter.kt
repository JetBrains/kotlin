// RUNTIME_WITH_FULL_JDK

import java.util.HashMap

fun foo(map : HashMap<String, Int>) {
    for (<caret>entry in map.entries) {

    }
}