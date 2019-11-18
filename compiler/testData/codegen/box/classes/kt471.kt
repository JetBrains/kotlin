// IGNORE_BACKEND_FIR: JVM_IR
class MyNumber(val i: Int) {
    operator fun inc(): MyNumber = MyNumber(i+1)
}

class MNR(var ref: MyNumber) {}

fun test1() : Boolean {
    var m  = MyNumber(42)

    m++
    if (m.i != 43) return false
    return true
}

fun test2() : Boolean {
    var m  = MyNumber(44)

    var m2 = m++
    if (m2.i != 44) return false
    if (m.i  != 45) return false
    return true
}

fun test3() : Boolean {
    var mnr  = MNR(MyNumber(42))
    mnr.ref++
    if (mnr.ref.i != 43) return false
    return true
}

fun test4() : Boolean {
    var mnr  = MNR(MyNumber(42))
    val m3 = mnr.ref++
    if (m3.i  != 42) return false
    return true
}

fun test5() : Boolean {
    var mnr  = Array<MyNumber>(2,{MyNumber(42)})
    mnr[0]++
    if (mnr[0].i  != 43) return false
    return true
}

fun test6() : Boolean {
    var mnr  = Array<MyNumber>(2,{it -> MyNumber(42-it)})
    mnr[1] = mnr[0]++
    if (mnr[0].i  != 43) return false
    if (mnr[1].i  != 42) return false
    return true
}

class MyArrayList<T>() {
    private var value17: T? = null
    private var value39: T? = null
    operator fun get(index: Int): T {
        if (index == 17) return value17!!
        if (index == 39) return value39!!
        throw Exception()
    }
    operator fun set(index: Int, value: T): T? {
        if (index == 17) value17 = value
        else if (index == 39) value39 = value
        else throw Exception()
        return null
    }
}

fun test7() : Boolean {
    var mnr  = MyArrayList<MyNumber>()
    mnr[17] = MyNumber(42)
    mnr[17]++
    if (mnr[17].i  != 43) return false
    return true
}

fun test8() : Boolean {
    var mnr  = MyArrayList<MyNumber>()
    mnr[17] = MyNumber(42)
    mnr[39] = mnr[17]++
    if (mnr[17].i  != 43) return false
    if (mnr[39].i  != 42) return false
    return true
}


fun box() : String {
    var m  = MyNumber(42)

    if (!test1()) return "fail test 1"
    if (!test2()) return "fail test 2"
    if (!test3()) return "fail test 3"
    if (!test4()) return "fail test 4"

    if (!test5()) return "fail test 5"
    if (!test6()) return "fail test 6"
    if (!test7()) return "fail test 7"
    if (!test8()) return "fail test 8"


    ++m
    if (m.i != 43) return "fail 0"

    var m1 = ++m
    if (m1.i != 44) return "fail 3"
    if (m.i  != 44) return "fail 4"

    return "OK"
}
