import kotlinx.cinterop.*

fun main(args: Array<String>) {
    memScoped {
        val count = 5

        val values = allocArray<IntVar>(count)
        values[0] = 14
        values[1] = 12
        values[2] = 9
        values[3] = 13
        values[4] = 8

        cstdlib.qsort(values, count.toLong(), IntVar.size, staticCFunction { a, b ->
            val aValue = a!!.reinterpret<IntVar>()[0]
            val bValue = b!!.reinterpret<IntVar>()[0]

            (aValue - bValue)
        })

        for (i in 0 .. count - 1) {
            print(values[i])
            print(" ")
        }
        println()
    }
}
