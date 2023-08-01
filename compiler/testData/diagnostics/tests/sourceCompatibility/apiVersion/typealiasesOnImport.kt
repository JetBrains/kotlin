// FIR_IDENTICAL
// !API_VERSION: 1.0

// FILE: a.kt
package a

@SinceKotlin("1.1")
class Since_1_1

typealias Since_1_1_Alias = <!API_NOT_AVAILABLE!>Since_1_1<!>

@SinceKotlin("1.1")
typealias Alias_1_1 = String

// FILE: b.kt
package b

import a.<!API_NOT_AVAILABLE!>Since_1_1_Alias<!>
import a.<!API_NOT_AVAILABLE!>Alias_1_1<!>