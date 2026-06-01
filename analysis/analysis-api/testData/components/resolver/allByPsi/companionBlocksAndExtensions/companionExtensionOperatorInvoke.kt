// LANGUAGE: +CompanionBlocksAndExtensions
package test

class C

companion operator fun C.invoke() {}

fun usage() {
    C()
    C.invoke()
}

// IGNORE_STABILITY: candidates
// ^KT-86685: for `C()` the resolved call is the constructor, but candidate collection yields the companion extension `invoke`.
