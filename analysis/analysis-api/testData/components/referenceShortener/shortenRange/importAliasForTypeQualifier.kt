// FILE: main.kt
package test

import com.dependency.Bar as Bar1

fun foo() = <expr>com.dependency.Bar.bar()</expr>

// FILE: dependency.kt
package com.dependency

class Bar {
    companion object
}

fun Bar.bar(): Bar {
    return this
}