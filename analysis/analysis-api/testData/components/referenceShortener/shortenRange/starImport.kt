// FILE: main.kt
package test

import com.dependency.*

fun foo() = <expr>com.dependency.bar()</expr>

// FILE: dependency.kt
package com.dependency

fun bar() = 3