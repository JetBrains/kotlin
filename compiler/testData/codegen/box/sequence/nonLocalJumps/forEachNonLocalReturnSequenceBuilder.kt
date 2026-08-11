// WITH_STDLIB
fun find(): String {
    sequence { yield(1); yield(2) }.forEach { if (it == 2) return "OK" }
    return "Fail"
}
fun box(): String = find()
