// WITH_RUNTIME
import java.io.File
import java.io.FilenameFilter

fun foo(filter: FilenameFilter) {}

fun bar() {
    foo(<caret>object: FilenameFilter {
        override fun accept(file: File, name: String) = true
    })
}
