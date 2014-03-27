import java.io.File

fun f(p1: Any, p2: String, p3: File) {
    File(<caret>)
}

// ABSENT: p1
// EXIST: p2
// EXIST: p3
