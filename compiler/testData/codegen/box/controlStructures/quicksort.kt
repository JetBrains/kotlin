// IGNORE_BACKEND_FIR: JVM_IR
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
        if (i <= j) {
            swap(i++, j--)
        }
        else {
        }
    }
    if (L < j)
        quicksort(L, j)
    if (R > i)
        quicksort(i, R)
}

fun box() : String {
    val a = IntArray(10)
    for(i in 0..4) {
        a[2*i]   =  2*i
        a[2*i+1] = -2*i-1
    }
    a.quicksort()
    for(i in 0..a.size-2) {
        if (a[i] > a[i+1]) return "Fail $i: ${a[i]} > ${a[i+1]}"
    }
    return "OK"
}
