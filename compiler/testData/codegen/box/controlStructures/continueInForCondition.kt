// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

import java.util.Arrays

fun foo(): List<String>? = Arrays.asList("abcde")

fun box(): String {
    for (i in 1..3) {
        for (value in foo() ?: continue) {
            if (value != "abcde") return "Fail"
        }
    }
    return "OK"
}
