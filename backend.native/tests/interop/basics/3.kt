import kotlinx.cinterop.*

fun main(args: Array<String>) {
    memScoped {
        val count = 5

        val values = allocArray<CInt32Var>(count)
        values[0].value = 14
        values[1].value = 12
        values[2].value = 9
        values[3].value = 13
        values[4].value = 8

        cstdlib.qsort(values[0].ptr, count.toLong(), CInt32Var.size, staticCFunction(::comparator))

        for (i in 0 .. count - 1) {
            print(values[i].value)
            print(" ")
        }
        println()
    }
}

private fun comparator(a: COpaquePointer?, b: COpaquePointer?): Int {
    val aValue = a!!.reinterpret<CInt32Var>().pointed.value
    val bValue = b!!.reinterpret<CInt32Var>().pointed.value

    return (aValue - bValue)
}
