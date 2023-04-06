// DUMP_LOCAL_DECLARATION_SIGNATURES

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57430

class Wrapper {
    private val dummy = object : Bar {}
    private val bar = object : Bar by dummy {}
}

interface Bar {
    val foo: String
        get() = ""
}
