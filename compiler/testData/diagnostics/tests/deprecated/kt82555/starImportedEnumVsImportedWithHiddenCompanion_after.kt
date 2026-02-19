// FIR_IDENTICAL
//  ^ K1 is ignored
// LANGUAGE: +SkipHiddenObjectsInResolution
// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP

// FILE: some.kt

package some

class Some {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    companion object
}

// FILE: main.kt

import some.Some
import E.*

enum class E {
    Some;
    fun foo() { }
}

fun test() {
    Some
    Some.foo()
    Some::foo
    Some::class
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, functionDeclaration */
