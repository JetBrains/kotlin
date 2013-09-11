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
