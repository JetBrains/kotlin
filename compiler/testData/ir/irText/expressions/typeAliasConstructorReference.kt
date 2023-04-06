// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

import Host.Nested

class C(x: Int)

typealias CA = C

object Host {
    class Nested(x: Int)
}

typealias NA = Nested

val test1: (Int) -> CA = ::CA
val test2: (Int) -> NA = ::NA
