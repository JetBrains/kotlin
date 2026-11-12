// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
package lib

const val C_BOOLEAN: Boolean = true
const val C_CHAR: Char = 'c'
const val C_BYTE: Byte = -1
const val C_SHORT: Short = 2

// MODULE: main(lib)
// FILE: main.kt
package app

import lib.*

const val and: Boolean = C_BOOLEAN && true
const val not: Boolean = !C_BOOLEAN
const val charPlus: Char = C_CHAR + 1
const val charMinus: Int = C_CHAR - 'a'
const val bytePlus: Int = C_BYTE + 1
const val shortPlus: Int = C_SHORT + 1
