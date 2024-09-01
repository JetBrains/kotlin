// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JVM_IR
// ^ @AssociatedObjectKey is not available in Kotlin/JVM

// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ISSUE: KT-70132

// MODULE: lib1
// FILE: lib1.kt
import kotlin.reflect.*

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
annotation class Annotation1(val kClass: KClass<out Any>)

// MODULE: lib2
// FILE: lib2.kt
import kotlin.reflect.*

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
annotation class Annotation2(val kClass: KClass<out Any>)

// MODULE: main(lib1, lib2)
// FILE: main.kt
@file:OptIn(ExperimentalAssociatedObjects::class)

import kotlin.reflect.*

@Annotation1(Outer.Inner1.Companion::class)
class Outer {
    class Inner1 {
        companion object {}
    }

    @Annotation2(Outer.Inner2.Companion::class)
    class Inner2 {
        companion object {}
    }
}

fun box(): String {
    if (Outer::class.findAssociatedObject<Annotation1>() != Outer.Inner1.Companion) return "fail1"
    if (Outer.Inner2::class.findAssociatedObject<Annotation2>() != Outer.Inner2.Companion) return "fail2"
    return "OK"
}