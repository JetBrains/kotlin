// IGNORE_BACKEND_FIR: JVM_IR
// KT-5869

operator fun <T> Iterator<T>.iterator(): Iterator<T> = this

fun box(): String {
    val iterator = object : Iterator<Int> {
        var i = 0
        override fun next() = i++
        override fun hasNext() = i < 5
    }

    var result = ""
    for (i in iterator) {
        result += i
    }

    return if (result == "01234") "OK" else "Fail $result"
}
