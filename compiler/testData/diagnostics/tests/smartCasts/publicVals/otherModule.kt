// MODULE: m1
// FILE: a.kt

package a

public class X {
    public val x : String? = null
}

// MODULE: m2(m1)
// FILE: b.kt

package b

import a.X

public fun X.gav(): Int {
    if (x != null)
        // Smart cast is not possible if definition is in another module
        return <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    else
        return 0
}

// FILE: c.kt

package a

public fun X.gav(): Int {
    if (x != null)
        // Even if it's in the same package
        return <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    else
        return 0
}
