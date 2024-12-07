// MODULE: sub
// FILE: sub.kt
package foo

class Base {
    @RequiresOptIn
    annotation class My

    @My
    @Deprecated("Yes")
    fun test() = "OK"
}

// MODULE: dep(sub)
// FILE: box.kt
package some

import foo.Base

@OptIn(Base.My::class)
fun box() = Base().test()
