import java.util.HashMap

fun foo() {
    val v = HashMap<<caret>
}

// EXIST: String
// EXIST: java
// ABSENT: defaultBufferSize
// ABSENT: readLine
