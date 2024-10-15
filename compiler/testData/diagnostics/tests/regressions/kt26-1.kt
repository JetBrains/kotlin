// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: this.kt

// KT-26 Import namespaces defined in this file
package foo

import bar.* // Must not be an error

// FILE: other.kt
package bar
