// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

interface Map {
    val size: Int
}

expect class HashMap : Map {
    override val size: Int
}

expect abstract class AbstractMap : Map {
    override val size: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual abstract class AbstractMap() : Map {
    actual override val size: Int = 0
}

actual class HashMap : AbstractMap(), Map
