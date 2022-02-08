// C
// WITH_STDLIB

class C {
    companion object {
        @[kotlin.jvm.JvmField] public val foo: String = { "A" }()
    }
}

// FIR_COMPARISON