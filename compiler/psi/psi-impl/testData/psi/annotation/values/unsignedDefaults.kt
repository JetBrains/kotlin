// Since 2.2, the metadata keeps the default values of annotation parameters, with unsigned values flagged as such
annotation class Defaults(
    val ub: UByte = UByte.MAX_VALUE,
    val us: UShort = UShort.MAX_VALUE,
    val ui: UInt = UInt.MAX_VALUE,
    val ul: ULong = ULong.MAX_VALUE,
)
