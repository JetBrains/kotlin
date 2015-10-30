// WITH_RUNTIME
import java.io.File
import java.io.FileFilter

fun foo(filter: FileFilter) {}

fun bar() {
    foo(<caret>object: FileFilter {
        override fun accept(file: File): Boolean {
            val name = file.name
            when (name) {
                "foo" -> return true

                "bar" -> return false

                else -> {
                    if (name.startsWith("a")) return true
                    return false
                }
            }
        }
    })
}
