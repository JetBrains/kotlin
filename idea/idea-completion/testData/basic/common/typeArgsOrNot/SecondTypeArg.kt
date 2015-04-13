import java.util.HashMap

fun foo() {
    val v = HashMap<String, <caret>
}

// EXIST: String
// EXIST: java
