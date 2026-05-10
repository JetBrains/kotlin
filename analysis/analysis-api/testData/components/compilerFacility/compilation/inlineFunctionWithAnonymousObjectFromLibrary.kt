// DUMP_IR

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
package com.example.library

interface Message {
    val text: String
}

inline fun printMessage(message: Message) {
    println(message.text)
}

inline fun greet(message: (String) -> String) {
    val name = "world"
    printMessage(object : Message {
        override val text = name
    })
}

// MODULE: main(library)
// FILE: main.kt
package test

import com.example.library.greet

fun test() {
    greet { "Hello, $it!" }
}
