fun test1() : Boolean {
    val r1 = 1..10
    var s1 = 0
    for(e in r1 step 2) {
        s1 += e
    }
    return s1 == 25
}

fun test2() : Boolean {
    val r1 = 1.byt..10.byt
    var s1 = 0
    for(e in r1 step 2) {
        s1 += e
    }
    return s1 == 25
}

fun test3() : Boolean {
    val r1 = 1.byt..10.lng
    var s1 = 0.lng
    for(e in r1 step 2) {
        s1 += e
    }
    return s1 == 25.lng
}

fun test4() : Boolean {
    val r1 = 1.byt..10.sht
    var s1 = 0.sht
    for(e in r1 step 2) {
        s1 += e
    }
    return s1 == 25.sht
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