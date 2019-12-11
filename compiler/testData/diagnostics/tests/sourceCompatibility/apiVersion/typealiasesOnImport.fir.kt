// !API_VERSION: 1.0

// FILE: a.kt
package a

@SinceKotlin("1.1")
class Since_1_1

typealias Since_1_1_Alias = Since_1_1

@SinceKotlin("1.1")
typealias Alias_1_1 = String

// FILE: b.kt
package b

import a.Since_1_1_Alias
import a.Alias_1_1