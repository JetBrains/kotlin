// FILE: main.kt
package test

import com.dependency.Bar as Bar1

fun foo(): <expr>com.dependency.Bar</expr> = Bar1()

// FILE: dependency.kt
package com.dependency

class Bar