// !DIAGNOSTICS: -EXPOSED_PARAMETER_TYPE

private class S public constructor() {
    fun a() {

    }
}

internal inline fun x(s: S, z: () -> Unit) {
    z()
    S()
    s.a()
    test()
}

private inline fun x2(s: S, z: () -> Unit) {
    z()
    S()
    s.a()
    test()
}

private fun test(): S {
    return S()
}