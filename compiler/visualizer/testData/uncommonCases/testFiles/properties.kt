package org.jetbrains.kotlin.test

val p1 = 10
val p2: Double = 1.0
val p3: Float = 2.5f
val p4 = "some string"

val p5 = p1 + p2
val p6 = p1 * p2 + (p5 - p3)

val withGetter
    get() = p1 * p3

var withSetter
    get() = p4
    set(value) = value

val withGetter2: Boolean
    get() {
        return true
    }

var withSetter2: String
    get() = "1"
    set(value) {
        field = value + "!"
    }

private val privateGetter: String = "cba"
    get

var privateSetter: String = "abc"
    private set

