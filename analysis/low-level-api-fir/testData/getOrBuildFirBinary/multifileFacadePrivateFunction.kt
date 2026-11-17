// MAIN_FILE_NAME: MultifileClass
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtNamedFunction

// FILE: file1.kt
@file:JvmMultifileClass
@file:JvmName("MultifileClass")
package one

private fun privateFunction1() {}

// FILE: file2.kt
@file:JvmMultifileClass
@file:JvmName("MultifileClass")
package one

private fun privateFunction2() {}
