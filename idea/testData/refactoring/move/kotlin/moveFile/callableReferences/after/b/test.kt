package b

import a.f2
import c.f4

fun test() {
    val ref1 = ::f1
    val ref2 = ::f2
    val ref3 = ::f3
    val ref4 = ::f4
}

fun f1() = 1