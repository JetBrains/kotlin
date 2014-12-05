import java.util.HashMap

fun foo() {
    val v = HashMap<List<(s: String?) -> Unit>, Set<<caret>
}

// EXIST: String
// EXIST: java
// ABSENT: defaultBufferSize
// ABSENT: readLine
