// WITH_RUNTIME
// IS_APPLICABLE: false
import java.io.File

val File.absolutePath: String get() = ""

fun foo(file: File) {
    file.getAbsolutePath()<caret>
}