// This file is compiled into each stepping test only if the WITH_STDLIB directive IS specified.

package testUtils

import kotlin.collections.AbstractMutableMap

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
internal val stdlibFqNames = mapOf(
    Pair::class to "kotlin.Pair",
    Triple::class to "kotlin.Triple",
    HashMap::class to "kotlin.collections.HashMap",
)
