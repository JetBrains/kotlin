// C
// WITH_RUNTIME

class C {
    companion object {
        @[kotlin.jvm.JvmField] public val foo: String = { "A" }()
    }
}

// FIR_COMPARISON