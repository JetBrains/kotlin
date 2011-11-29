namespace quicksort

fun IntArray.swap(i:Int, j:Int) {
    val temp = this[i]
    this[i] = this[j]
    this[j] = temp
}

fun IntArray.quicksort() = quicksort(0, size-1)

fun IntArray.quicksort(L: Int, R:Int) {
    val m = this[(L + R) / 2]
    var i = L
    var j = R
    while (i <= j) {
        while (this[i] < m)
            i++
        while (this[j] > m)
            j--
        if (i <= j) { KT-5
            swap(i++, j--)
        }
    }
    if (L < j)
        quicksort(L, j)
    if (R > i)
        quicksort(i, R)
}

fun main(array: Array<String>) {
    val a = IntArray(100000000)
    var i = 0
    val len = a.size
    while (i < len) {
        a[i] = i * 3 / 2 + 1
        if (i % 3 == 0)
            a[i] = -a[i]
        i++
    }

    val start = System.currentTimeMillis()

    a.quicksort()

    val total = System.currentTimeMillis() - start
    System.out?.println("[Quicksort-" + System.getProperty("project.name")+ " Benchmark Result: " + total + "]");
}
