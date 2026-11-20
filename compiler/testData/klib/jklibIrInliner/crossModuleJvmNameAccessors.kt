// MODULE: lib
// FILE: lib.kt
package lib

var topLevel: Int = 0
    @JvmName("readTopLevel") get
    @JvmName("writeTopLevel") set

class Holder {
    var member: Int = 0
        @JvmName("readMember") get
        @JvmName("writeMember") set
}

inline fun bumpTopLevel() {
    topLevel = topLevel + 1
}

inline fun bumpMember(holder: Holder) {
    holder.member = holder.member + 1
}

// MODULE: main(lib)
// FILE: main.kt
package test

import lib.Holder
import lib.bumpMember
import lib.bumpTopLevel

fun test(holder: Holder) {
    bumpTopLevel()
    bumpMember(holder)
}
