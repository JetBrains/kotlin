// FILE: main.kt
package test

import com.dependency.*
import com.dependency.bar as bar1
import com.dependency.bar as bar2
import com.dependency.bar as bar3
import com.dependency.bar

fun foo(a: Int) = <expr>when (a) {
    1 -> bar1
    2 -> bar2
    3 -> bar3
    else -> com.dependency.bar
}</expr>

// FILE: dependency.kt
package com.dependency

val bar = 3