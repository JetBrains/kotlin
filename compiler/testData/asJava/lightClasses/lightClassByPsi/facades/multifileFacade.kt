// LIBRARY_PLATFORMS: JVM
// DECLARATIONS_NO_LIGHT_ELEMENTS: multifileFacade.class[privateFoo], multifileFacade__MultifileFacadeKt.class[privateFoo;x1], multifileFacade__SecondMultifileFacadeKt.class[y1]
// LIGHT_ELEMENTS_NO_DECLARATION: multifileFacade__MultifileFacadeKt.class[privateFoo$multifileFacade__MultifileFacadeKt]

// FILE: MultifileFacade.kt
@file:JvmMultifileClass
@file:JvmName("multifileFacade")

fun foo() = 42

val x = 24

private fun privateFoo(): Int = 3

const val x1 = 42

// FILE: SecondMultifileFacade.kt
@file:JvmMultifileClass
@file:JvmName("multifileFacade")

fun bar() = 24

val y = 24

const val y1 = 42
