// "Replace with 'absolutePath'" "true"
import java.io.File

@Deprecated("", ReplaceWith("absolutePath"))
val File.prop: String
    get() = absolutePath

fun foo(file: File) {
    file.prop<caret>
}

