// FILE: main.kt
package test

import com.dependency.bar as bar1

fun foo() = <expr>com.dependency.bar()</expr>

// FILE: dependency.kt
package com.dependency

fun bar() = 3