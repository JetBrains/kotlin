// EMIT_JVM_TYPE_ANNOTATIONS
// JVM_DEFAULT_MODE: no-compatibility
// JVM_TARGET: 1.8
// RENDER_ANNOTATIONS
// WITH_STDLIB
// LANGUAGE: +JvmEnhancedBridges

// MODULE: lib
// FILE: lib.kt

package lib

@Target(AnnotationTarget.FUNCTION)
internal annotation class InternalTargetAnno

interface A<T> {
    fun foo(t: T)
}

open class B {
    @InternalTargetAnno
    fun foo(t: String) {}
}

// MODULE: main(lib)
// FILE: main.kt

package main

import lib.*

class J: B(), A<String> {
    // bridge with InternalTargetAnno is expected. Java also copies package-private annotations in similar cases
}