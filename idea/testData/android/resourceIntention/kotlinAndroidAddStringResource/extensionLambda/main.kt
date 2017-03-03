package com.myapp

import android.content.Context

fun foo(context: Context) {
    with (context) {
        with (2) {
            "z<caret>xc"
        }
    }
}

inline fun <T, R> with(receiver: T, block: T.() -> R): R = receiver.block()
