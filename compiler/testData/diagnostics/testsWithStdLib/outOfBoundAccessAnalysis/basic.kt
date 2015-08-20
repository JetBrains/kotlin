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
    val arr = IntArray(5)
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
    val lst = listOf(1, 2, 3, 4, 5)
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

fun accessThroughMethods() {
    val arr = IntArray(2)
    arr.set(0, 1)
    val fst = arr.get(1)
    val snd = arr.<!OUT_OF_BOUND_ACCESS!>get(2)<!>
    arr.<!OUT_OF_BOUND_ACCESS!>set(3, fst + snd)<!>
}

fun multiSizeArray(cond: Boolean) {
    val arr: Array<Int>
    if (cond) {
        arr = Array(3, { it })
    }
    else {
        arr = Array(5, { it })
    }

    if (arr.size() == 5) {
        for (i in 3 .. 4) {
            arr[i] = 0
        }
    }

    if (arr.size() == 3) {      // typo, `3` instead `5`
        for (i in 3 .. 4) {
            <!OUT_OF_BOUND_ACCESS!>arr[i] = 0<!>
        }
    }

    if (arr.size() == 3) {
        for (i in 0 .. 3) {     // typo, `3` instead `2`
            println(<!OUT_OF_BOUND_ACCESS!>arr[i]<!>)
        }
    }
}

fun insideLambda(cond: Boolean) {
    var a: Int? = null
    if (cond) {
        a = 3
    }
    a?.let {
        val arr = arrayOf(1, 2, 3)
        println(arr[it])                // no alarm here, `it` is lambda's argument and is unknown
        val n = 3
        println(<!OUT_OF_BOUND_ACCESS!>arr[n]<!>)                 // local variable's value is known
    }
    a?.let { i ->
        val arr = arrayOf(1, 2, 3)
        println(arr[i])                 // same as `it`
    }
    val outArr = arrayOf(1, 2, 3)
    val k = 3
    a?.let {
        val n = 3
        println(<!OUT_OF_BOUND_ACCESS!>outArr[n]<!>)
        println(<!OUT_OF_BOUND_ACCESS!>outArr[k]<!>)
    }
}