// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT
class C1 {
    val o = "O"
}

class C2 {
    val k = "K"
}

class E {
    class I
}

context(c1: C1, c2: C2)
companion fun E.foo() = c1.o + c2.k

context(c1: C1, c2: C2)
companion val E.a
    get() = c1.o + c2.k

context(c1: C1, c2: C2)
companion fun E.I.bar() = c1.o + c2.k

context(c1: C1, c2: C2)
companion fun C2.baz() = c1.o + c2.k

fun box(): String {
    with(C1()) {
        with(C2()) {
            if (E.foo() == "OK" && E.I.bar() == "OK" && C2.baz() == "OK")
                return E.a
            else return "FAIL"
        }
    }
}
