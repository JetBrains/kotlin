// KJS_WITH_FULL_RUNTIME
fun for_int_range(): Int {
    var c = 0
    loop@ for (i in 1..10) {
        if (c >= 5) continue@loop
        c++
    }
    return c
}

fun for_byte_range(): Int {
    var c = 0
    val from: Byte = 1
    val to: Byte = 10
    loop@ for (i in from..to) {
        if (c >= 5) continue@loop
        c++
    }
    return c
}

fun for_long_range(): Int {
    var c = 0
    val from: Long = 1
    val to: Long = 10
    loop@ for (i in from..to) {
        if (c >= 5) continue@loop
        c++
    }
    return c
}

fun for_int_list(): Int {
    val a = ArrayList<Int>()
    a.add(0); a.add(0); a.add(0); a.add(0); a.add(0)
    a.add(0); a.add(0); a.add(0); a.add(0); a.add(0)
    var c = 0
    loop@ for (i in a) {
        if (c >= 5) continue@loop
        c++
    }
    return c
}

fun for_byte_list(): Int {
    val a = ArrayList<Byte>()
    a.add(0); a.add(0); a.add(0); a.add(0); a.add(0)
    a.add(0); a.add(0); a.add(0); a.add(0); a.add(0)
    var c = 0
    loop@ for (i in a) {
        if (c >= 5) continue@loop
        c++
    }
    return c
}

fun for_long_list(): Int {
    val a = ArrayList<Long>()
    a.add(0); a.add(0); a.add(0); a.add(0); a.add(0)
    a.add(0); a.add(0); a.add(0); a.add(0); a.add(0)
    var c = 0
    loop@ for (i in a) {
        if (c >= 5) continue@loop
        c++
    }
    return c
}

fun for_double_list(): Int {
    val a = ArrayList<Double>()
    a.add(0.0); a.add(0.0); a.add(0.0); a.add(0.0); a.add(0.0)
    a.add(0.0); a.add(0.0); a.add(0.0); a.add(0.0); a.add(0.0)
    var c = 0
    loop@ for (i in a) {
        if (c >= 5) continue@loop
        c++
    }
    return c
}

fun for_object_list(): Int {
    val a = ArrayList<Any>()
    a.add(0.0); a.add(0.0); a.add(0.0); a.add(0.0); a.add(0.0)
    a.add(0.0); a.add(0.0); a.add(0.0); a.add(0.0); a.add(0.0)
    var c = 0
    loop@ for (i in a) {
        if (c >= 5) continue@loop
        c++
    }
    return c
}

fun for_str_array(): Int {
    val a = arrayOfNulls<String>(10)
    var c = 0
    loop@ for (i in a) {
        if (c >= 5) continue@loop
        c++
    }
    return c
}

fun for_intarray(): Int {
    val a = IntArray(10)
    var c = 0
    loop@ for (i in a) {
        if (c >= 5) continue@loop
        c++
    }
    return c
}

fun box(): String {
    if (for_int_range() != 5) return "fail 1"
    if (for_byte_range() != 5) return "fail 2"
    if (for_long_range() != 5) return "fail 3"
    if (for_intarray() != 5) return "fail 4"
    if (for_str_array() != 5) return "fail 5"
    if (for_int_list() != 5) return "fail 6"
    if (for_byte_list() != 5) return "fail 7"
    if (for_long_list() != 5) return "fail 8"
    if (for_double_list() != 5) return "fail 9"
    return "OK"
}
