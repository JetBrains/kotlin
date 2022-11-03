// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FILE: A.kt

package test

@Deprecated("A")
interface A

// FILE: B.kt

import test.<!DEPRECATION!>A<!>
