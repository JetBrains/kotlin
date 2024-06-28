// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

var log = "begin"
fun append(msg: String) {
    log = "$log;$msg"
}

val test1 get() = run {
    append("test1.get")
    "1"
}

val test2 get() = run {
    append("test2.get")
    test1
}

var test3: String = "Z"
    set(value) {
        append("test3.set")
        field = value
    }

test3 = "3"

val r = "$test1;$test2;$test3|$log"
// expected: r: 1;1;3|begin;test3.set;test1.get;test2.get;test1.get