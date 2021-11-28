import kotlin.collections.*

@CompileTimeCalculation
class A(val a: Int) {
    override fun equals(other: Any?): Boolean {
        return other is Int && other == a
    }
}
const val customEquals1 = <!EVALUATED: `true`!>A(1) == 1<!>
const val customEquals2 = <!EVALUATED: `false`!>A(1) == 123<!>
const val customEquals3 = <!EVALUATED: `false`!>1 == A(1)<!>
const val customEquals4 = <!EVALUATED: `false`!>123 == A(1)<!>
const val customEquals5 = <!EVALUATED: `false`!>null == A(1)<!>
const val customEquals6 = <!EVALUATED: `false`!>A(1) == null<!>

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
