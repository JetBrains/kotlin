// IGNORE_FE10
// MODULE: dep1
// FILE: util11.kt
package my.pack

fun Any.myExt() {}

// FILE: util12.kt
package my.pack

val Any.myExt: Int get() = 10

// MODULE: dep2
// FILE: util21.kt
package my.pack

fun Any.myExt2() {}

// FILE: util22.kt
package my.pack

val Any.myExt2: Int get() = 10

// MODULE: main(dep1, dep2)
// FILE: usage.kt
package usage

import my.pack.myExt
import my.pack.myExt2

fun test(a: Any) {
    a.myExt()
    a.myExt

    a.myExt2
    a.myExt2()
}
