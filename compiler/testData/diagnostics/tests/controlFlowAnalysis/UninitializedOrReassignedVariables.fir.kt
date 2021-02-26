// !WITH_NEW_INFERENCE
package uninitialized_reassigned_variables

fun doSmth(s: String) {}
fun doSmth(i: Int) {}

// ------------------------------------------------
// uninitialized variables

fun t1(b : Boolean) {
    val v : Int
    if (<!UNINITIALIZED_VARIABLE!>v<!> == 0) {}

    var u: String
    if (b) {
        u = "s"
    }
    doSmth(<!UNINITIALIZED_VARIABLE!>u<!>)

    var r: String
    if (b) {
        r = "s"
    }
    else {
        r = "tg"
    }
    doSmth(r)

    var t: String
    if (b)
        doSmth(<!UNINITIALIZED_VARIABLE!>t<!>)
    else
        t = "ss"
    doSmth(<!UNINITIALIZED_VARIABLE!>t<!>) //repeat for t

    val i = 3
    doSmth(i)
    if (b) {
        return;
    }
    doSmth(i)
    if (i is Int) {
        return;
    }
}

fun t2() {
    val s = "ss"

    for (i in 0..2) {
        doSmth(s)
    }
}

class A() {}

fun t4(a: A) {
    a = A()
}

// ------------------------------------------------
// reassigned vals

fun t1() {
    val a : Int = 1
    <!VAL_REASSIGNMENT!>a<!> = 2

    var b : Int = 1
    b = 3
}

enum class ProtocolState {
  WAITING {
    override fun signal() = ProtocolState.TALKING
  },

  TALKING {
    override fun signal() = ProtocolState.WAITING
  };

  abstract fun signal() : ProtocolState
}

fun t3() {
   val x: ProtocolState = ProtocolState.WAITING
   <!VAL_REASSIGNMENT!>x<!> = x.signal()
   <!VAL_REASSIGNMENT!>x<!> = x.signal() //repeat for x
}

fun t4() {
    val x = 1
    <!VARIABLE_EXPECTED!>x<!> += 2
    val y = 3
    <!VARIABLE_EXPECTED!>y<!> *= 4
    var z = 5
    z -= y
}

fun t5() {
    for (i in 0..2) {
        <!VARIABLE_EXPECTED!>i<!> += 1
        fun t5() {
            <!VARIABLE_EXPECTED!>i<!> += 3
        }
    }
}

// ------------------------------------------------
// backing fields

var x = 10
val y = 10
val z = 10

class AnonymousInitializers(var a: String, val b: String) {
    init {
        a = "30"
        a = "s"

        b = "3"
        b = "tt" //repeat for b
    }

    val i: Int
    init {
        i = 121
    }

    init {
        x = 11
        z = 10
    }

    val j: Int
    get() = 20

    init {
        i = 13
        j = 34
    }

    val k: String
    init {
        if (1 < 3) {
            k = "a"
        }
        else {
            k = "b"
        }
    }

    val l: String
    init {
        if (1 < 3) {
            l = "a"
        }
        else {
            l = "b"
        }
    }

    val o: String
    init {
        if (1 < 3) {
            o = "a"
        }
    }

    var m: Int = 30

    init {
        m = 400
    }

    val n: Int

    init {
        while (n == 0) {
        }
        n = 10
        while (n == 0) {
        }
    }

    var p = 1
    init {
        p++
    }
}

fun reassignFunParams(a: Int) {
    a = 1
}

open class Open(a: Int, w: Int) {}

class LocalValsVsProperties(val a: Int, w: Int) : Open(a, w) {
    val x : Int
    val y : Int
    init {
        x = 1
        val b = x
    }
    val b = a

    fun foo() {
        val r : Int
        doSmth(x)
        doSmth(y)
        doSmth(<!UNINITIALIZED_VARIABLE!>r<!>)
        doSmth(a)
    }
    var xx = w
    var yy : Int
    init {
        <!VARIABLE_EXPECTED!>w<!> += 1
        yy = w
    }
}

class Outer() {
    val a : Int
    var b : Int

    init {
        a = 1
        b = 1
    }

    inner class Inner() {
        init {
            a++
            b++
        }
    }

    fun foo() {
        a++
        b++
    }
}

class ForwardAccessToBackingField() { //kt-147
    val a = a // error
    val b = c // error
    val c = 1
}

class ClassObject() {
    companion object {
        val x : Int

        init {
            x = 1
        }


        fun foo() {
            val a : Int
            doSmth(<!UNINITIALIZED_VARIABLE!>a<!>)
        }
    }
}

fun foo() {
    val a = object {
        val x : Int
        val y : Int
        val z : Int
        init {
            x = 1
            z = 3
        }
        fun foo() {
            y = 10
            z = 13
        }
    }
}

class TestObjectExpression() {
    val a : Int
    fun foo() {
        val a = object {
            val x : Int
            val y : Int
            init {
                if (true)
                    x = 12
                else
                    x = 1
            }
            fun inner1() {
                y = 101
                a = 231
            }
            fun inner2() {
                y = 101
                a = 231
            }
        }
    }
}



object TestObjectDeclaration {
    val x : Int
    val y : Int
    init {
        x = 1
    }

    fun foo() {
        y = 10
        val i: Int
        if (1 < 3) {
            i = 10
        }
        doSmth(<!UNINITIALIZED_VARIABLE!>i<!>)
    }
}

fun func() {
    val b = 1
    val a = object {
        val x = b
        init {
            <!VAL_REASSIGNMENT!>b<!> = 4
        }
    }
}

// ------------------------------------------------
// dot qualifiers
class M() {
    val x = 11
    var y = 12
}

fun test(m : M) {
    m.x = 23
    m.y = 23
}

fun test1(m : M) {
    m.x++
    m.y--
}
