import kotlin.*
import kotlin.collections.*

@CompileTimeCalculation
class A(var num: Int, var str: String) {
    fun setNewStr(newString: String) {
        this.str = newString
    }
}

@CompileTimeCalculation
fun <T> echo(value: T): T = value

const val a = <!EVALUATED: `Run block`!>run { echo("Run block") }<!>

const val b = A(0, "Run with receiver").<!EVALUATED: `Run with receiver0`!>run { this.str + this.num }<!>

const val c = <!EVALUATED: `New String`!>with (A(1, "String")) {
    setNewStr("New String")
    this.str
}<!>

const val d = A(2, "Apply test").apply { this.setNewStr("New apply str") }.<!EVALUATED: `New apply str`!>str<!>

const val e = mutableListOf("one", "two", "three").also { it.add("four") }.<!EVALUATED: `4`!>size<!>
const val f1 = mutableListOf("one", "two", "three").<!EVALUATED: `4`!>let {
    it.add("four")
    it.size
}<!>
const val f2 = 10.<!EVALUATED: `20`!>let { it + 10 }<!>

const val g1 = 1.takeIf { it % 2 == 0 }.<!EVALUATED: `null`!>toString()<!>
const val g2 = 2.takeIf { it % 2 == 0 }.<!EVALUATED: `2`!>toString()<!>
const val h1 = (-1).takeUnless { it > 0 }.<!EVALUATED: `-1`!>toString()<!>
const val h2 = 1.takeUnless { it > 0 }.<!EVALUATED: `null`!>toString()<!>
