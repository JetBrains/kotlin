// FILE: Arrays.kt
annotation class Arrays(
    val ia: IntArray,
    val la: LongArray,
    val fa: FloatArray,
    val da: DoubleArray,
    val ca: CharArray,
    val ba: BooleanArray
)

// FILE: WithArrays.kt
@Arrays(
    [1, 2, 3],
    [1L],
    [],
    [2.2],
    ['a'],
    [true, false]
)
class WithArrays

// FILE: WithExplicitArrays.kt
@Arrays(
    intArrayOf(1, 2, 3),
    longArrayOf(1L),
    floatArrayOf(),
    doubleArrayOf(2.2),
    charArrayOf('a'),
    booleanArrayOf(true, false),
)
class WithExplicitArrays
