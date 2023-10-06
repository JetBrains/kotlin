// ISSUE: KT-62183

package kotlin

public open class Any {
    fun toString(): String { return this.toString() }
    open operator fun equals(other: Any?): Boolean { return this.equals(other) }
}

public class String {
    operator fun plus(other: Any?): String { return this.plus(other) }
}

public class Boolean {
    operator fun not(): Boolean { return this.not() }
}

public class Int {
    operator fun plus(other: Int): Int { return this.plus(other) }
    operator fun times(other: Int): Int { return this.times(other) }
    infix fun xor(other: Int): Int { return this.xor(other) }
}

public object Unit {
}
