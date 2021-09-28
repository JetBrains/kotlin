fun use(f: C.(Int) -> Unit) {}

class C

fun C.extensionVararg(i: Int, vararg s: String) {}

fun C.extensionDefault(i: Int, s: String = "") {}

fun C.extensionBoth(i: Int, s: String = "", vararg t: String) {}

fun testExtensionVararg() {
    use(C::extensionVararg)
}

fun testExtensionDefault() {
    use(C::extensionDefault)
}

fun testExtensionBoth() {
    use(C::extensionBoth)
}
