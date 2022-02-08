// FIR_IDENTICAL
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