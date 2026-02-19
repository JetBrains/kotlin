// MAIN_FILE_NAME: MultifileClass__File2Kt
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty
// COMPILER_ARGUMENTS: -Xmultifile-parts-inherit

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
