// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface IFoo {
    fun foo(k: String): String
}

fun fooK(iFoo: IFoo) = iFoo.foo("K")

fun box() =
    fooK {
        fooK {
            fooK { k -> "O" + k }
        }
    }