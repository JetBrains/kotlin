// IGNORE_BACKEND_FIR: JVM_IR
fun t1 () {
    val a1 = arrayOfNulls<String>(1)
    a1[0] = "0" //ok
    val s = a1[0] //ok
}

fun t2 () {
    val a2 = arrayOfNulls<Int>(1) as Array<Int>
    a2[0] = 0 //ok
    var i = a2[0] //ok
}

fun t3 () {
    val a3 = arrayOfNulls<Int>(1)
    a3[0] = 0 //verify error
    var j = a3[0] //ok
    var k : Int = a3[0] ?: 5 //ok
}

fun t4 () {
    val b1 = StrangeIntArray(10)
    b1[4] = 5 //ok
    var i = b1[1] //ok
}

fun t5 () {
    val b2 = StrangeArray<Int>(10, 0)
    b2.set(4, 5) //ok
    b2[4] = 5 //verify error
    var i = b2.get(2) //ok
    i = b2[1] //verify error
}

fun t6() {
    val b3 = StrangeArray<Int?>(10, 0)
    b3.set(5, 6) //ok
    b3[4] = 5 //verify error
    val v = b3[1] //ok
}

fun box() : String {
    return "OK"
}

class StrangeArray<T>(size: Int, private var defaultValue: T) {
    operator fun get(index: Int): T = defaultValue
    operator fun set(index: Int, v: T) {
        defaultValue = v
    }
}

class StrangeIntArray(size: Int) {
    private var defaultValue = 0
    operator fun get(index: Int): Int = defaultValue
    operator fun set(index: Int, v: Int) {
        defaultValue = v
    }
}
