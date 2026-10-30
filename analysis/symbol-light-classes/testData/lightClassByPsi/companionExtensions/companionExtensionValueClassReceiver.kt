// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

package one

@JvmInline
value class C(val value: String)

companion fun C.companionExt(): Int = 1

companion var C.companionExtVar: Int
    get() = 1
    set(value) {}

// The receiver is not a parameter, so there is nothing to box
@JvmExposeBoxed
companion fun C.exposed(): Int = 1

// A top-level declaration is not mangled because of its return type
companion fun C.returnsC(): C = C("")

// A value class in a value parameter still mangles the name
companion fun C.takesC(c: C): Int = 1

companion var C.current: C
    get() = C("")
    set(value) {}

// DECLARATIONS_NO_LIGHT_ELEMENTS: CompanionExtensionValueClassReceiverKt.class[takesC]
// LIGHT_ELEMENTS_NO_DECLARATION: C.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], CompanionExtensionValueClassReceiverKt.class[setCurrent-x_RLm0c;takesC-x_RLm0c]
