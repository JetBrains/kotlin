// FILE: dependency.kt
package one.two

enum class Color { Black, White, Blue, Red, Green }

// FILE: main.kt
package main

import one.two.Color
import one.two.Color.Black
import one.two.Color.Green
import one.two.Color.*
