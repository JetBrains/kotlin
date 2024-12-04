package lib

interface I1 {
    fun i1() {}
}

interface I2 {
    fun i2() {}
}

interface I3 : I2, I1

open class C {
    fun c() {}
}

open class G<T> {
    fun g() {}
}

private val o1 = object { fun foo() {} }
private val o2 = object : I1 {}
private val o3 = object : I1, I2 {}
private val o4 = object : I3 {}
private val o5 = object : C() {}
private val o6 = object : C(), I1, I2 {}
private val o7 = object : C(), I3 {}
private val o8 = object : G<Int>() {}
private val o9 = object : G<Int>(), I1, I2 {}
private val o10 = object : G<Int>(), I3 {}

private val o11 = object {
    inner class D {
        fun df() {}
    }
    fun d(): D = D()
}.d()

private val o12 = {
    class L {
        fun l() {}
    }
    L()
}()

private val o13 = {
    class L {
        inner class L1 {
            inner class L2 {
                fun l2() {}
            }
        }
    }

    L().L1().L2()
}()

fun fn() {
    o1.foo()
    o2.i1()
    o3.i1()
    o3.i2()
    o4.i1()
    o4.i2()
    o5.c()
    o6.c()
    o6.i1()
    o6.i2()
    o7.c()
    o7.i1()
    o7.i2()
    o8.g()
    o9.g()
    o9.i1()
    o9.i2()
    o10.g()
    o10.i1()
    o10.i2()
    o11.df()
    o12.l()
    o13.l2()
}

class W {
    private val o1 = object { fun foo() {} }
    private val o2 = object : I1 {}
    private val o3 = object : I1, I2 {}
    private val o4 = object : I3 {}
    private val o5 = object : C() {}
    private val o6 = object : C(), I1, I2 {}
    private val o7 = object : C(), I3 {}
    private val o8 = object : G<Int>() {}
    private val o9 = object : G<Int>(), I1, I2 {}
    private val o10 = object : G<Int>(), I3 {}

    fun w() {
        o1.foo()
        o2.i1()
        o3.i1()
        o3.i2()
        o4.i1()
        o4.i2()
        o5.c()
        o6.c()
        o6.i1()
        o6.i2()
        o7.c()
        o7.i1()
        o7.i2()
        o8.g()
        o9.g()
        o9.i1()
        o9.i2()
        o10.g()
        o10.i1()
        o10.i2()
    }
}

object O {
    private val o1 = object { fun foo() {} }
    private val o2 = object : I1 {}
    private val o3 = object : I1, I2 {}
    private val o4 = object : I3 {}
    private val o5 = object : C() {}
    private val o6 = object : C(), I1, I2 {}
    private val o7 = object : C(), I3 {}
    private val o8 = object : G<Int>() {}
    private val o9 = object : G<Int>(), I1, I2 {}
    private val o10 = object : G<Int>(), I3 {}

    fun o() {
        o1.foo()
        o2.i1()
        o3.i1()
        o3.i2()
        o4.i1()
        o4.i2()
        o5.c()
        o6.c()
        o6.i1()
        o6.i2()
        o7.c()
        o7.i1()
        o7.i2()
        o8.g()
        o9.g()
        o9.i1()
        o9.i2()
        o10.g()
        o10.i1()
        o10.i2()
    }
}
