package test

interface I {
    fun foo(): Int
}

fun usage(delegate: I): I = <caret>object : I by delegate {}
