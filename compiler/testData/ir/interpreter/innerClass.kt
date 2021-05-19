@CompileTimeCalculation
class Outer {
    private val bar: String = "bar"
    val num = 1

    fun foo() = "outer foo"

    inner class Middle {
        val num = 2

        fun foo() = "middle foo"

        inner class Inner {
            val num = 3

            fun foo() = "inner foo with outer bar = \"$bar\""

            fun getAllNums() = "From inner: " + this.num + "; from middle: " + this@Middle.num + "; from outer: " + this@Outer.num
        }
    }
}

const val a1 = Outer().<!EVALUATED: `outer foo`!>foo()<!>
const val a2 = Outer().Middle().<!EVALUATED: `middle foo`!>foo()<!>
const val a3 = Outer().Middle().Inner().<!EVALUATED: `inner foo with outer bar = "bar"`!>foo()<!>

const val b = Outer().Middle().Inner().<!EVALUATED: `From inner: 3; from middle: 2; from outer: 1`!>getAllNums()<!>
