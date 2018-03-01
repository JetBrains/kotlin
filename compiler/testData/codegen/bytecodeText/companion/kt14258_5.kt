// Checks that accessor are not used because property can be accessed directly.

interface I {
    companion object {
        private val bar = "Companion Field from I"

        fun test(): String {
            return bar
        }
    }
}

// 1 GETSTATIC I\$Companion.bar
// 1 PUTSTATIC I\$Companion.bar
// 0 INVOKESTATIC I\$Companion.access\$getBar\$p
// 0 INVOKESTATIC I\$Companion.access\$setBar\$cp
// 0 INVOKESPECIAL I\$Companion.getBar
// 0 INVOKESPECIAL I\$Companion.setBar