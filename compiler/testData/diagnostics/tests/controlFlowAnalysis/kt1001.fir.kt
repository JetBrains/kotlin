//KT-1001 Argument 2 for @NotNull parameter of JetFlowInformationProvider.checkIsInitialized must not be null

package kt1001

fun foo(c: Array<Int>) {
    return

    for (i in c) {}
    for (i in c) {}
}

//more tests

fun t1() : Int {
    try {
        return 1
    }
    catch (e : Exception) {
        return 2
    }
    return 3
}

fun t2() : Int {
    try {
        return 1
    }
    finally {
        doSmth()
    }
    return 2
}

fun doSmth() {}
