// FIR_COMPARISON
import java.io.File

fun File.foo(absolutePath: String?) {
    <caret>
}

// EXIST: getAbsolutePath
// ABSENT: { itemText: "absolutePath", typeText: "String" }
