package com.example.ictest

inline fun calculate(): Int {
    val callable = {
        40 + 2
    }
    return callable()
}
