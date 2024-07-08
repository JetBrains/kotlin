// IGNORE_BACKEND: NATIVE

// FILE: file1.kt
package org.sample

private var fileNameVar = "file1.kt "

internal inline var fileNameInlineVar1: String
    get() = fileNameVar
    set(value) { fileNameVar = value }

// FILE: file2.kt
package org.sample

private var fileNameVar = "file2.kt "

internal inline var fileNameInlineVar2: String
    get() = fileNameVar
    set(value) { fileNameVar = value }

// FILE: main.kt
import org.sample.*

fun box(): String {
    var result = ""
    result += fileNameInlineVar1
    result += fileNameInlineVar2
    if (result != "file1.kt file2.kt ") return result
    result = ""
    fileNameInlineVar1 = "1! "
    fileNameInlineVar2 = "2! "
    result += fileNameInlineVar1
    result += fileNameInlineVar2
    if (result != "1! 2! ") return result
    return "OK"
}
