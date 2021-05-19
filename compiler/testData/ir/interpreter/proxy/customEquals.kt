import kotlin.collections.*

@CompileTimeCalculation
class B(val b: Int) {
    override fun equals(other: Any?): Boolean {
        other as? B ?: return false
        return this.b == other.b
    }

    override fun toString(): String = "B($b)"
}
const val areEquals = <!EVALUATED: `true`!>listOf(B(1), B(2)) == listOf(B(1), B(2))<!>
const val asString = listOf(B(1), B(2)).<!EVALUATED: `[B(1), B(2)]`!>toString()<!>
