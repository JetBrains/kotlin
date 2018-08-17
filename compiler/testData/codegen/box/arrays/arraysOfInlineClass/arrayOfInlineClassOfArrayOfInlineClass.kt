// !LANGUAGE: +InlineClasses
// WITH_UNSIGNED
// IGNORE_BACKEND: JVM_IR, JS_IR

inline class Data(val data: Array<UInt>)

val D =
    Array(4) { i ->
        Data(Array(4) { j ->
            (i + j).toUInt()
        })
    }

fun box(): String {
    for (i in D.indices) {
        for (j in D[i].data.indices) {
            val x = D[i].data[j].toInt()
            if (x != i + j) throw AssertionError()
        }
    }

    return "OK"
}