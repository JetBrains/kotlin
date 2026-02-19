private class A

private inline fun privateInlineFun1() {
    A()
}

private inline fun privateInlineFun2() {
    privateInlineFun1()
    A()
}

private inline fun privateInlineFun3() = privateInlineFun2()

internal inline fun internalInlineFun() = privateInlineFun3()
