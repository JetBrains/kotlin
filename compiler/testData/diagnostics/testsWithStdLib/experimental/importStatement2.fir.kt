// KT-59582

// FILE: a.kt
@RequiresOptIn
annotation class Ann()

// FILE: b.kt
package b

import Ann
