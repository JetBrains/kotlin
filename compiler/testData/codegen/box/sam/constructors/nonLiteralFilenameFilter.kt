// TARGET_BACKEND: JVM

import java.io.*

fun box(): String {
    val ACCEPT_NAME = "test"
    val WRONG_NAME = "wrong"

    val f : (File?) -> Boolean = { file -> ACCEPT_NAME == file?.getName() }
    val filter = FileFilter(f)

    if (!filter.accept(File(ACCEPT_NAME))) return "Wrong answer for $ACCEPT_NAME"
    if (filter.accept(File(WRONG_NAME))) return "Wrong answer for $WRONG_NAME"

    return "OK"
}
