// WITH_STDLIB
fun find(): String {
    sequence { yield(1); yield(2) }.first { if (it == 2) return "OK"; false }
    return "Fail"
}
fun box(): String = find()
