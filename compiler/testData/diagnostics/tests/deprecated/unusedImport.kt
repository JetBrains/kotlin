// RUN_PIPELINE_TILL: BACKEND
// FILE: A.kt

package test

@Deprecated("A")
interface A

// FILE: B.kt

import test.<!DEPRECATION!>A<!>
