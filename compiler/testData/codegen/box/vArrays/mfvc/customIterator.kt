import java.lang.StringBuilder

// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER
// TARGET_FRONTEND: FIR
// LANGUAGE: +ValueClasses

@JvmInline
value class Point(val x: Int, val y: Int)

class PointIterator(val array: VArray<Point>) : VArrayIterator<Point> {
    private var index: Int = 0

    override fun hasNext() = index < array.size

    override fun next() = array[index++]
}

fun box(): String {

    val arr = VArray(2) { Point(it, it + 1) }

    val it = PointIterator(arr)

    val builder = StringBuilder()

    while (it.hasNext()) {
        builder.append(it.next())
    }

    if (builder.toString() != "Point(x=0, y=1)Point(x=1, y=2)") return "Fail"

    return "OK"
}