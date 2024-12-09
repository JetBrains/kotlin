// LANGUAGE: +ValhallaValueClasses
// IGNORE_BACKEND_K1: ANY
// ENABLE_JVM_PREVIEW
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: ANDROID
// IGNORE_DEXING
// CHECK_BYTECODE_LISTING

import java.util.Objects

value class Rational(val m: Long, val n: Long) {
    init {
        require(n > 0) { "Illegal natural number $n." }
    }

    private fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
    
    override fun equals(other: Any?): Boolean {
        if (other !is Rational) return false
        val thisGcd = gcd(this.m, this.n)
        val otherGcd = gcd(other.m, other.n)
        return this.m / thisGcd == other.m / otherGcd && this.n / thisGcd == other.n / otherGcd
    }

    override fun hashCode(): Int {
        val gcd = gcd(m, n)
        return Objects.hash(m / gcd, n / gcd)
    }
}

infix fun Long.ratDiv(other: Long) = Rational(this, other)

value class Wrapper<T>(val x: T)

fun box(): String {
    val x = 2L ratDiv 3L
    val y = 4L ratDiv 6L
    
    require(x == y) { "$x != $y" }
    
    val wrapperX = Wrapper(x)
    val wrapperY = Wrapper(y)
    require(wrapperX == wrapperY) { "$wrapperX != $wrapperY" }
    
    return "OK"
}
