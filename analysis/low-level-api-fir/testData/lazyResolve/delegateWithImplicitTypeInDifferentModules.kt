// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MODULE: m1
// FILE: Interface.kt
interface Interface {
    fun implicitType() = 42
}

// MODULE: m2(m1)
// MEMBER_NAME_FILTER: implicitType
// FILE: usage.kt
class Aa<caret>a(i: Interface) : Interface by i
