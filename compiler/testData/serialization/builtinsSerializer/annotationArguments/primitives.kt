package test

annotation class Primitives(
        val byte: Byte,
        val char: Char,
        val short: Short,
        val int: Int,
        val long: Long,
        val float: Float,
        val double: Double,
        val boolean: Boolean
)

@Primitives(
        byte = 7,
        char = '%',
        short = 239,
        int = 239017,
        long = 123456789123456789L,
        float = 2.72f,
        double = -3.14,
        boolean = true
)
class C

@Primitives(
        byte = 7: Byte,
        char = '%': Char,
        short = 239: Short,
        int = 239017: Int,
        long = 123456789123456789L: Long,
        float = 2.72f: Float,
        double = -3.14: Double,
        boolean = true: Boolean
)
class D

