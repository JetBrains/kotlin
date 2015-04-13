import java.util.HashMap

fun foo() {
    val v = HashMap<String, <caret>
}

// INVOCATION_COUNT: 2
// ELEMENT: HashSet