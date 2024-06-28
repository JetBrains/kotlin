// IGNORE_FE10
// KT-64503

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
interface KotlinForDelegationInterface {
    fun justFun()
}

open class KotlinAbsImplKtInterface(val delegate: KotlinForDelegationInterface) : KotlinForDelegationInterface by delegate

// MODULE: main(lib)
// FILE: usage.kt
class UseLibDelegate(private val b: KotlinForDelegationInterface) : KotlinAbsImplKtInterface(b)
