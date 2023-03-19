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
