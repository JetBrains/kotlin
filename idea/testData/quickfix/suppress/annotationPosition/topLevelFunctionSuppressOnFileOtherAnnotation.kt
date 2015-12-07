// "Suppress 'REDUNDANT_NULLABLE' for file ${file}" "true"
// ERROR: This annotation is not applicable to target 'file' and use site target '@file'

@file:Deprecated("Some")

package test

public fun foo(): String?<caret>? = null