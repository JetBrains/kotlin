// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

fun s0() : Boolean {
    val y = "222"
    val foo = {
        val bar = { y }
        bar ()
    }
    return foo() == "222"
}

fun s1() : Boolean {
    var x = "222"
    val foo = {
        val bar = {
            x = "aaa"
        }
        bar ()
    }
    foo()
    return x == "aaa"
}

fun t1() : Boolean {
    var x = "111"

    val y = x + "22"
    val foo = {
        x = x + "45" + y
        x = x.substring(3)
        x += "aaa"
        Unit
    }
    foo()

    x += "bbb"
    System.out?.println(x)
    return x == "4511122aaabbb"
}

fun t2() : Boolean {
    var x = 111
    val y = x + 22
    val foo = {
        x = x + 5 + y
        x += 5
        x++
        Unit
    }
    foo()
    x -= 55
    System.out?.println(x)
    return x == 200
}

fun t3() : Boolean {
    var x = true
    val foo = {
        x = false
        Unit
    }
    foo()
    return !x
}

fun t4() : Boolean {
    var x = 100.toFloat()
    val y = x + 22
    val foo = {
        x = x + 200.toFloat() + y
        x += 18
        Unit
    }
    foo()
    System.out?.println(x)
    return x == 440.toFloat()
}

fun t5() : Boolean {
    var x = 100.toDouble()
    val y = x + 22
    val foo = {
        x = x + 200.toDouble() + y
        x -= 22
        Unit
    }
    foo()
    System.out?.println(x)
    return x == 400.toDouble()
}

fun t6() : Boolean {
    var x = 20.toByte()
    val y = x + 22
    val foo = {
        x = (x + 20.toByte() + y).toByte()
        x = (x + 2).toByte()
        x--
        Unit
    }
    foo()
    System.out?.println(x)
    return x == 83.toByte()
}

fun t7() : Boolean {
    var x : Char = 'a'
    val foo = {
        x = 'b'
        Unit
    }
    foo()
    System.out?.println(x)
    return x == 'b'
}

fun t8() : Boolean {
    var x = 20.toShort()
    val foo = {
        val bar = {
            x = 30.toShort()
            Unit
        }
        bar()
        Unit
    }
    foo()
    return x == 30.toShort()
}

fun t9(x0: Int) : Boolean {
    var x = x0
    while(x < 100) {
       x++
    }
    return x == 100
}

fun t10() : Boolean {
    var y = 1
    val foo = {
        val bar = {
            y = y + 1
        }
        bar()
    }
    foo()
    return y == 2
}

fun t11(x0: Int) : Int {
    var x = x0
    val foo = {
        x = x + 1
        val bar = {
            x = x + 1
            x += 3
        }
        bar()
    }
    while(x < 100) {
       foo()
    }
    return x
}

fun t12(x: Int) : Int {
    var y = x
    val runnable = object : Runnable {
        override fun run () {
            y = y + 1
        }
    }
    while(y < 100) {
       runnable.run()
    }
    return y
}

fun box(): String {
    if (!s0()) return "s0 fail"
    if (!s1()) return "s1 fail"
    if (!t1()) return "t1 fail"
    if (!t2()) return "t2 fail"
    if (!t3()) return "t3 fail"
    if (!t4()) return "t4 fail"
    if (!t5()) return "t5 fail"
    if (!t6()) return "t6 fail"
    if (!t7()) return "t7 fail"
    if (!t8()) return "t8 fail"
    if (!t9(0)) return "t9 fail"
    if (!t10()) return "t10 fail"
    if (t11(1) != 101) return "t11 fail"
    if (t12(0) != 100) return "t12 fail"

    return "OK"
}
