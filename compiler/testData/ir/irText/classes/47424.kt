// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

package com.example


interface Aa

interface Ab<T : Ab<T>> : Aa


interface Ba

interface Bb<T : Bb<T>> : Ab<T>, Ba


interface Ca {
    val b: Ba
}

interface Cb {
    val b: Bb<*>
}

interface C : Cb, Ca
