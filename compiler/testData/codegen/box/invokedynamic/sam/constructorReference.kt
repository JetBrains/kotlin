// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

class C(val test: String)

fun interface MakeC {
    fun make(x: String): C
}

fun make(makeC: MakeC) = makeC.make("OK")

fun box() = make(::C).test