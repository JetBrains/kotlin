fun test1() : Boolean {
    val r1 = 1..10
    var s1 = 0
    for(e in r1 step 2) {
        s1 += e
    }
    return s1 == 25
}

fun test2() : Boolean {
    val r1 = 1.toByte()..10.toByte()
    var s1 = 0
    for(e in r1 step 2) {
        s1 += e
    }
    return s1 == 25
}

fun test3() : Boolean {
    val r1 = 1.toByte()..10.toLong()
    var s1 = 0.toLong()
    for(e in r1 step 2) {
        s1 += e
    }
    return s1 == 25.toLong()
}

fun test4() : Boolean {
    val r1 = 1.toByte()..10.toShort()
    var s1 = 0.toShort()
    for(e in r1 step 2) {
        s1 += e
    }
    return s1 == 25.toShort()
}

fun test5() : Boolean {
    val r1 = 'a'..'h'
    var s1 = 0
    for(e in r1 step 2) {
        s1 ++
    }
    return s1 == 4
}

fun box() : String {
    if(test1().not()) return "test1 failed"
    if(test2().not()) return "test2 failed"
    if(test3().not()) return "test3 failed"
    if(test4().not()) return "test4 failed"
    if(test5().not()) return "test4 failed"

    return "OK"
}
