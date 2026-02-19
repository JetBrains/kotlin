// MAIN_FILE_NAME: MultifileClass__File2Kt
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty
// COMPILER_ARGUMENTS: -Xmultifile-parts-inherit

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
