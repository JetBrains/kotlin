package test

annotation class IntAnno
annotation class StringAnno
annotation class DoubleAnno

[IntAnno] val Int.extension: Int
    get() = this

[StringAnno] val String.extension: String
    get() = this

[DoubleAnno] val Double.extension: Int
    get() = 42
