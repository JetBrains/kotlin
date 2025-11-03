// LANGUAGE: +HoldsInContracts, +AllowContractsOnSomeOperators
// IGNORE_FE10
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: declaration.kt
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
import kotlin.contracts.*

inline operator fun Boolean.invoke(block:()-> Unit) {
    contract { this@invoke holdsIn block }
}

// MODULE: main(lib)
// FILE: main.kt
fun test(x: String?){
    (x is String) {
        x.length
    }
}
