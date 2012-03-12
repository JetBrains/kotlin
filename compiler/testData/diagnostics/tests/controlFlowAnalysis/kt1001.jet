//KT-1001 Argument 2 for @NotNull parameter of JetFlowInformationProvider.checkIsInitialized must not be null

package kt1001

fun foo(<!UNUSED_PARAMETER!>c<!>: Array<Int>) {
    return

    <!UNREACHABLE_CODE!>for (i in c) {}<!>
    <!UNREACHABLE_CODE!>for (i in c) {}<!>
}

//more tests

fun t1() : Int {
    try {
        return 1
    }
    catch (e : Exception) {
        return 2
    }
    <!UNREACHABLE_CODE!>return 3<!>
}

fun t2() : Int {
    try {
        return 1
    }
    finally {
        doSmth()
    }
    <!UNREACHABLE_CODE!>return 2<!>
}

fun doSmth() {}
