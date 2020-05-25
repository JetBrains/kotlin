// PROBLEM: none
// RUNTIME_WITH_FULL_JDK
import java.util.concurrent.ConcurrentHashMap

fun test(map: ConcurrentHashMap<Int, String>) {
    map.<caret>forEach(1) { key, value ->
        foo(key, value)
    }
}

fun foo(i: Int, s: String) {}