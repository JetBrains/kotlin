fun arrayAccessWithLiteral(): Int {
    val arr = arrayOf(1)
    val a = arr[0]
    val b = <!OUT_OF_BOUND_ACCESS!>arr[1]<!>
    return a + b
}

fun simpleFor() {
    val arr = IntArray(5)
    for (i in 0..4) {
        println(arr[i])
    }
    for (i in 0..5) {
        println(<!OUT_OF_BOUND_ACCESS!>arr[i]<!>)
    }
}

fun arraySizeMethodCall() {
    val arr = IntArray(3)
    for (i in 0 .. arr.size()) {
        println(<!OUT_OF_BOUND_ACCESS!>arr[i]<!>)
    }
    for (i in 0 .. 5) {
        if (i < arr.size()) {
            println(arr[i])
        }
        if (i <= arr.size()) {
            println(<!OUT_OF_BOUND_ACCESS!>arr[i]<!>)
        }
    }
}

fun listAccessWithLiteral(): Int {
    val lst = listOf(1)
    val a = lst[0]
    val b = <!OUT_OF_BOUND_ACCESS!>lst[1]<!>
    val lst2 = arrayListOf(1, 2)
    val c = lst2[1]
    val d = <!OUT_OF_BOUND_ACCESS!>lst2[2]<!>
    return a + b + c + d
}

fun listSizeMethodCall() {
    val lst = listOf(1, 2, 3)
    for (i in 0 .. lst.size()) {
        println(<!OUT_OF_BOUND_ACCESS!>lst[i]<!>)
    }
    for (i in 0 .. 5) {
        if (i < lst.size()) {
            println(lst[i])
        }
        if (i <= lst.size()) {
            println(<!OUT_OF_BOUND_ACCESS!>lst[i]<!>)
        }
    }
}