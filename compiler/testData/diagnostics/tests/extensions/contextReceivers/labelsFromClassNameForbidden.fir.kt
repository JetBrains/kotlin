// ISSUE: KT-63068
fun List<Int>.f() {
    this@List.size
}

context(String)
fun Int.f() {
    this@String.length
    this@Int.toDouble()
}

context(String)
val p: String get() = this@String
