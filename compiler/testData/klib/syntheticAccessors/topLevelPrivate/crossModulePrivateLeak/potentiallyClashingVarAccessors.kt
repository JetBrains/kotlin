// IGNORE_BACKEND: NATIVE

// MODULE: lib1
// FILE: file1.kt
package org.sample

private var libName = "lib1 "

internal inline var libNameInlineVar1: String
    get() = libName
    set(value) { libName = value }

// MODULE: lib2
// FILE: file2.kt
package org.sample

private var libName = "lib2 "

internal inline var libNameInlineVar2: String
    get() = libName
    set(value) { libName = value }

// MODULE: main()(lib1, lib2)
// FILE: main.kt
import org.sample.*

fun box(): String {
    var result = ""
    result += libNameInlineVar1
    result += libNameInlineVar2
    if (result != "lib1 lib2 ") return result
    result = ""
    libNameInlineVar1 = "1! "
    libNameInlineVar2 = "2! "
    result += libNameInlineVar1
    result += libNameInlineVar2
    if (result != "1! 2! ") return result
    return "OK"
}
