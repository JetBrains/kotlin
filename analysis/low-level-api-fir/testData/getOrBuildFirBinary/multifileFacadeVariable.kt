// MAIN_FILE_NAME: MultifileClass
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty

// FILE: file1.kt
@file:JvmMultifileClass
@file:JvmName("MultifileClass")
package one

var publicVariable1: Int = 0

// FILE: file2.kt
@file:JvmMultifileClass
@file:JvmName("MultifileClass")
package one

var publicVariable2: Int = 0
