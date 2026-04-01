// TARGET_BACKEND: JVM

import java.util.HashSet

interface A : Set<String>

class B : A, HashSet<String>() {
    override fun clone(): B = throw AssertionError()
}

fun box(): String {
    return try {
        B().clone()
        "Fail 1"
    } catch (e: AssertionError) {
        try {
            val hs: HashSet<String> = B()
            hs.clone()
            "Fail 2"
        } catch (e: AssertionError) {
            "OK"
        }
    }
}
