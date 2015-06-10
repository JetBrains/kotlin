// !CHECK_TYPE

import java.io.File

fun test() {
    val dir = File("dir")
    val files = dir.listFiles()?.toList() ?: listOf() // error
    files checkType { _<List<File>>() }
}