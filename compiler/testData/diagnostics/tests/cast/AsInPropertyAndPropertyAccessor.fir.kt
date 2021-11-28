// !DIAGNOSTICS: -UNUSED_VARIABLE

fun test() {
    val a = 1 as Any?
    val b: Number = 1 as Number
    val c = null as String?
    val d: Number = 1 <!USELESS_CAST!>as Int<!>
}

val c1 get() = 1 as Number
val c2: Number get() = 1 as Number

val d: Number
    get() {
        1 as Number
        return 1 as Number
    }
