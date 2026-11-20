// COMPILER_ARGUMENTS: -Xmetadata-version=2.1.0
// Before 2.2, the default values of annotation parameters are kept only in the bytecode, where unsigned values are signed
annotation class Defaults(
    val ub: UByte = UByte.MAX_VALUE,
    val us: UShort = UShort.MAX_VALUE,
    val ui: UInt = UInt.MAX_VALUE,
    val ul: ULong = ULong.MAX_VALUE,
)
