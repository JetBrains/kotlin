fun test1(): Int {
    return     1
}

fun test2(): Int {
    return
    1
}

fun test3(): Int {
    return



    1
}

fun test4(): Int {
    return synchronized(this) {(): Int ->
        return@synchronized 12
    }
}

fun test5(): Int {
    return synchronized(this) {(): Int ->
        return     @synchronized      12
    }
}

fun test6(): Int {
    return synchronized(this) {(): Int ->
        return

        @synchronized

        12
    }
}

