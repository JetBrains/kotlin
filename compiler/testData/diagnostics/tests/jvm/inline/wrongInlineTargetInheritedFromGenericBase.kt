// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// DIAGNOSTICS: -NOTHING_TO_INLINE
// ISSUE: KT-90553

// MODULE: base11
// JVM_TARGET: 11
// FILE: base11.kt
package lib

open class Base11<T> {
    inline fun inlineFunction(value: T): T = value

    inline val inlineProperty: Int
        get() = 11
}

// MODULE: base17
// JVM_TARGET: 17
// FILE: base17.kt
package lib

open class Base17<T> {
    inline fun inlineFunction(value: T): T = value

    inline val inlineProperty: Int
        get() = 17
}

// MODULE: derived17(base11)
// JVM_TARGET: 17
// FILE: derived17.kt
package lib

class Derived17 : Base11<String>()

// MODULE: derived11(base17)
// JVM_TARGET: 11
// FILE: derived11.kt
package lib

class Derived11 : Base17<String>()

// MODULE: main(base11, base17, derived17, derived11)
// JVM_TARGET: 11
// FILE: main.kt
package main

import lib.*

fun usage(
    base11: Base11<String>,
    base17: Base17<String>,
    derived17: Derived17,
    derived11: Derived11,
) {
    base11.inlineFunction("base")
    base11.inlineProperty
    base17.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("base")
    base17.<!INLINE_FROM_HIGHER_PLATFORM!>inlineProperty<!>

    derived17.inlineFunction("inherited")
    derived17.inlineProperty

    derived11.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("inherited")
    derived11.<!INLINE_FROM_HIGHER_PLATFORM!>inlineProperty<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, inline, integerLiteral, nullableType,
propertyDeclaration, stringLiteral, typeParameter */
