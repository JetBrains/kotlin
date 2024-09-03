// WITH_STDLIB
// IGNORE_BACKEND: JVM

fun ubyte_rangeTo() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UByte.MIN_VALUE..UByte.MAX_VALUE) {
        ints.add(b.toInt())
        if (++counter > 256) error("Something went wrong")
    }
    require(ints == (0..255).toList()) { ints.toString() }
}

fun ubyte_downTo() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UByte.MAX_VALUE downTo UByte.MIN_VALUE) {
        ints.add(b.toInt())
        if (++counter > 256) error("Something went wrong")
    }
    require(ints == (255 downTo 0).toList()) { ints.toString() }
}

fun ubyte_rangeUntil() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UByte.MIN_VALUE..<UByte.MAX_VALUE) {
        ints.add(b.toInt())
        if (++counter > 255) error("Something went wrong")
    }
    require(ints == (0..<255).toList()) { ints.toString() }
}

fun ubyte_rangeTo2() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UByte.MIN_VALUE..UByte.MAX_VALUE step 2) {
        ints.add(b.toInt())
        if (++counter > 256) error("Something went wrong")
    }
    require(ints == (0..255 step 2).toList()) { ints.toString() }
}

fun ubyte_downTo2() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UByte.MAX_VALUE downTo UByte.MIN_VALUE step 2) {
        ints.add(b.toInt())
        if (++counter > 256) error("Something went wrong")
    }
    require(ints == (255 downTo 0 step 2).toList()) { ints.toString() }
}

fun ubyte_rangeUntil2() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UByte.MIN_VALUE..<UByte.MAX_VALUE step 2) {
        ints.add(b.toInt())
        if (++counter > 255) error("Something went wrong")
    }
    require(ints == (0..<255 step 2).toList()) { ints.toString() }
}

fun ubyte_rangeTo3() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UByte.MIN_VALUE..UByte.MAX_VALUE step 3) {
        ints.add(b.toInt())
        if (++counter > 256) error("Something went wrong")
    }
    require(ints == (0..255 step 3).toList()) { ints.toString() }
}

fun ubyte_downTo3() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UByte.MAX_VALUE downTo UByte.MIN_VALUE step 3) {
        ints.add(b.toInt())
        if (++counter > 256) error("Something went wrong")
    }
    require(ints == (255 downTo 0 step 3).toList()) { ints.toString() }
}

fun ubyte_rangeUntil3() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UByte.MIN_VALUE..<UByte.MAX_VALUE step 3) {
        ints.add(b.toInt())
        if (++counter > 255) error("Something went wrong")
    }
    require(ints == (0..<255 step 3).toList()) { ints.toString() }
}

fun ushort_rangeTo() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UShort.MIN_VALUE..UShort.MAX_VALUE) {
        ints.add(b.toInt())
        if (++counter > 65536) error("Something went wrong")
    }
    require(ints == (0..65535).toList()) { ints.toString() }
}

fun ushort_downTo() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UShort.MAX_VALUE downTo UShort.MIN_VALUE) {
        ints.add(b.toInt())
        if (++counter > 65536) error("Something went wrong")
    }
    require(ints == (65535 downTo 0).toList()) { ints.toString() }
}

fun ushort_rangeUntil() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UShort.MIN_VALUE..<UShort.MAX_VALUE) {
        ints.add(b.toInt())
        if (++counter > 65535) error("Something went wrong")
    }
    require(ints == (0..<65535).toList()) { ints.toString() }
}

fun ushort_rangeTo2() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UShort.MIN_VALUE..UShort.MAX_VALUE step 2) {
        ints.add(b.toInt())
        if (++counter > 65536) error("Something went wrong")
    }
    require(ints == (0..65535 step 2).toList()) { ints.toString() }
}

fun ushort_downTo2() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UShort.MAX_VALUE downTo UShort.MIN_VALUE step 2) {
        ints.add(b.toInt())
        if (++counter > 65536) error("Something went wrong")
    }
    require(ints == (65535 downTo 0 step 2).toList()) { ints.toString() }
}

fun ushort_rangeUntil2() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UShort.MIN_VALUE..<UShort.MAX_VALUE step 2) {
        ints.add(b.toInt())
        if (++counter > 65535) error("Something went wrong")
    }
    require(ints == (0..<65535 step 2).toList()) { ints.toString() }
}

fun ushort_rangeTo3() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UShort.MIN_VALUE..UShort.MAX_VALUE step 3) {
        ints.add(b.toInt())
        if (++counter > 65536) error("Something went wrong")
    }
    require(ints == (0..65535 step 3).toList()) { ints.toString() }
}

fun ushort_downTo3() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UShort.MAX_VALUE downTo UShort.MIN_VALUE step 3) {
        ints.add(b.toInt())
        if (++counter > 65536) error("Something went wrong")
    }
    require(ints == (65535 downTo 0 step 3).toList()) { ints.toString() }
}

fun ushort_rangeUntil3() {
    var counter = 0
    val ints = mutableListOf<Int>()
    for (b in UShort.MIN_VALUE..<UShort.MAX_VALUE step 3) {
        ints.add(b.toInt())
        if (++counter > 65535) error("Something went wrong")
    }
    require(ints == (0..<65535 step 3).toList()) { ints.toString() }
}

fun box(): String {
    ubyte_rangeTo()
    ubyte_downTo()
    ubyte_rangeUntil()
    
    ubyte_rangeTo2()
    ubyte_downTo2()
    ubyte_rangeUntil2()
    
    ubyte_rangeTo3()
    ubyte_downTo3()
    ubyte_rangeUntil3()
    
    
    ushort_rangeTo()
    ushort_downTo()
    ushort_rangeUntil()

    ushort_rangeTo2()
    ushort_downTo2()
    ushort_rangeUntil2()

    ushort_rangeTo3()
    ushort_downTo3()
    ushort_rangeUntil3()
    
    return "OK"
}
