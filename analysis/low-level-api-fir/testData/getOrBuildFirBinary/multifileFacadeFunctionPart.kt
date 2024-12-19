// MAIN_FILE_NAME: MultifileClass__File1Kt
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtNamedFunction

// FILE: file1.kt
@file:JvmMultifileClass
@file:JvmName("MultifileClass")
package one

fun publicFunction1() {}

// FILE: file2.kt
@file:JvmMultifileClass
@file:JvmName("MultifileClass")
package one

fun publicFunction2() {}
