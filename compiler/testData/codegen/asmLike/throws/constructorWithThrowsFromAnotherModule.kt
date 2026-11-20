// JVM_TARGET: 1.8
// RENDER_ANNOTATIONS
// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt

package lib

import java.io.IOException

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CONSTRUCTOR)
annotation class Retained

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CONSTRUCTOR)
annotation class SourceOnly

class A
@Retained
@SourceOnly
@Throws(IOException::class, IllegalStateException::class)
constructor()

// MODULE: main(lib)
// FILE: main.kt

package main

import lib.A

fun create(): A = A()
