import kotlinx.cinterop.*

fun main(args: Array<String>) {
    val values = intArrayOf(14, 12, 9, 13, 8)
    val count = values.size

    cstdlib.qsort(values.refTo(0), count.toLong(), IntVar.size, staticCFunction { a, b ->
        val aValue = a!!.reinterpret<IntVar>()[0]
        val bValue = b!!.reinterpret<IntVar>()[0]

        (aValue - bValue)
    })

    for (i in 0..count - 1) {
        print(values[i])
        print(" ")
    }
    println()
}
