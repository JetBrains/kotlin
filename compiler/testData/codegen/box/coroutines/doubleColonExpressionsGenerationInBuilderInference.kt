// IGNORE_BACKEND: JS, JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// WITH_RUNTIME

package a.b

class BatchInfo1(val batchSize: Int)
class BatchInfo2<T>(val data: T)

object Obj

fun test() {
    val a: Sequence<String> = sequence {
        val x = BatchInfo1::class
        val y = a.b.BatchInfo1::class
        val z = Obj::class

        val x1 = BatchInfo1::batchSize
        val y1 = a.b.BatchInfo1::class

        yieldAll(listOf(x, y, z, x1, y1).map { it.toString() })
    }

    val size = a.toList().size
    assert(size == 5) { "actual size: $size"}
}

fun box(): String {
    test()
    return "OK"
}