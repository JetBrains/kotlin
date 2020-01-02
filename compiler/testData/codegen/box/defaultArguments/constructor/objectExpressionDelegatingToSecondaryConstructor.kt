// IGNORE_BACKEND_FIR: JVM_IR

abstract class O(val value: String) {
    constructor(o: Char = 'O') : this("$o")
}

abstract class K {
    val value: String

    constructor(k: Char = 'K') {
        value = "$k"
    }
}

fun box(): String {

    val o = object : O() {}

    val k = object : K() {}

    return o.value + k.value
}