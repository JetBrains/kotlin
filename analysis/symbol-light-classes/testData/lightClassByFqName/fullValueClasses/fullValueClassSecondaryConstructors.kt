// pack.ValueClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
package pack

value class ValueClass(val first: Int, val second: String) {
    constructor(first: Int) : this(first, "")
    constructor(other: ValueClass) : this(other.first, other.second)
    private constructor() : this(0)
    internal constructor(second: String) : this(0, second)
    protected constructor(first: Long) : this(first.toInt())
}
