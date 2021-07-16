// FILE: A.kt

package test

@Deprecated("A")
interface A

// FILE: B.kt

import <!DEPRECATION!>test.A<!>
