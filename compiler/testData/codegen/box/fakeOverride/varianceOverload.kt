
// MODULE: lib
// FILE: lib.kt
// KT-43831


var result = "FAIL"

class Change : IsChange {

    override fun qux(select: RPPG1<out AC1>): IsChange? {
        result = ""
        return null
    }

    override fun foo(select: RPPG1<out AC1>, sss: RPPG1<in I1>): Change? {
        result += "O"
        return null
    }

    override fun bar(a: RPPG2<out AC1, in AC2<out I2>>, b: RPPG3<in AC1, AC2<in I2>, out AC3<in I2, out I3>>): Change? {
        result += "K"
        return null
    }

}

interface IsChange {
    fun qux(select: RPPG1<out AC1>): IsChange?

    fun foo(select: RPPG1<out AC1>, sss: RPPG1<in I1>): IsChange?


    fun bar(a: RPPG2<out AC1, in AC2<out I2>>, b: RPPG3<in AC1, AC2<in I2>, out AC3<in I2, out I3>>): IsChange?
}

abstract class AC1 : BAC1<Any>()
abstract class AC2<PP1: Any> : BAC2<Any, PP1>()
abstract class AC3<PP1: Any, PP2: Any> : BAC3<Any, PP1, PP2>()

interface I1
interface I2
interface I3

abstract class BAC1<D1 : Any> : I1
abstract class BAC2<D1 : Any, D2: Any> : I2
abstract class BAC3<D1 : Any, D2: Any, D3: kotlin.Any> : I3

class RPPG1<P1 : I1>
class RPPG2<P1 : I1, P2 : I2>
class RPPG3<P1 : I1, P2 : I2, P3 : I3>

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    val c = Change()
    c.qux(RPPG1())
    c.foo(RPPG1(), RPPG1())
    c.bar(RPPG2(), RPPG3())
    return result

}