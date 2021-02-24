// This test emulates 'allopen' compiler plugin.

@Suppress("INCOMPATIBLE_MODIFIERS")
open data class Test(
    open val x: String,
    open val y: String
)

// 1 GETFIELD Test\.x
// 1 GETFIELD Test\.y

// 6 INVOKEVIRTUAL Test\.getX
// 6 INVOKEVIRTUAL Test\.getY
// - componentN, copy$default, toString, hashCode, 2 times in equals