// FIR_IDENTICAL
// DUMP_LOCAL_DECLARATION_SIGNATURES

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57428

open class Base(val f1: () -> Any)

object Thing : Base({ Thing }) {
    fun test1() = Thing
    fun test2() = this
}
