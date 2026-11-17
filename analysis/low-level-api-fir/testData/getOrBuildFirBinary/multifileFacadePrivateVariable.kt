// MAIN_FILE_NAME: MultifileClass
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty

// FILE: file1.kt
@file:JvmMultifileClass
@file:JvmName("MultifileClass")
package one

private var privateVariable1: Long = 1L

// FILE: file2.kt
@file:JvmMultifileClass
@file:JvmName("MultifileClass")
package one

private var privateVariable2: Long = 1L
