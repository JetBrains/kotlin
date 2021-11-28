// FIR_IDENTICAL
// ISSUE: KT-46285

sealed class SealedClass {
    fun test() {
        val anon = object : <!SEALED_SUPERTYPE_IN_LOCAL_CLASS("Anonymous object; class")!>SealedClass<!>() {}
        class Local : <!SEALED_SUPERTYPE_IN_LOCAL_CLASS("Local class; class")!>SealedClass<!>()
    }
}

sealed interface SealedInterface {
    fun test() {
        val anon = object : <!SEALED_SUPERTYPE_IN_LOCAL_CLASS("Anonymous object; interface")!>SealedInterface<!> {}
        class Local : <!SEALED_SUPERTYPE_IN_LOCAL_CLASS("Local class; interface")!>SealedInterface<!>
    }
}
