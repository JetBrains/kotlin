// MAIN_FILE_NAME: MultifileClass
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty

// FILE: file1.kt
@file:JvmMultifileClass
@file:JvmName("MultifileClass")
package one

internal var internalVariable1: String = "str"

// FILE: file2.kt
@file:JvmMultifileClass
@file:JvmName("MultifileClass")
package one

internal var internalVariable2: String = "str"
