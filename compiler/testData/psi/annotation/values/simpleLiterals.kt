// FILE: Simple.kt
annotation class Simple(
    val i: Int,
    val l: Long,
    val b: Byte,

    val d: Double,
    val f: Float,

    val c: Char,

    val b1: Boolean,
    val b2: Boolean,
)

// FILE: WithSimple.kt
@Simple(
    12,
    12L,
    12,

    3.3,
    3.3F,

    'a',

    true,
    false
)
class WithSimple

// FILE: WithNamedSimple.kt
@Simple(
    12,
    12L,
    12,

    d = 3.3,
    f = 3.3F,

    c = 'a',

    b1 = true,
    b2 = false
)
class WithNamedSimple

// FILE: WithSimpleOperations.kt
@Simple(
    12 / 6,
    12L % 5L,
    12,

    3.3 - 3.0,
    3.3F * 2.0F,

    'a',

    true && false,
    false || true,
)
class WithSimpleOperations
