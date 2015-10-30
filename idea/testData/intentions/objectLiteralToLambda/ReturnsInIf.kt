// WITH_RUNTIME
import java.io.File
import java.io.FileFilter

fun foo(filter: FileFilter) {}

fun bar() {
    foo(<caret>object: FileFilter {
        override fun accept(file: File): Boolean {
            val name = file.name
            if (name.startsWith("a")) {
                return false
            }
            else {
                if (name.endsWith("b"))
                    return true
                else {
                    val l = name.length
                    return l > 10
                }
            }
        }
    })
}
