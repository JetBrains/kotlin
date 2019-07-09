// IGNORE_BACKEND: JVM_IR
// Checks that accessor are not used because property can be accessed directly.

interface I {
    companion object {
        private var bar = "Companion Field from I"

        fun test(): String {
            bar = "New value"
            return bar
        }
    }
}

// 1 GETSTATIC I\$Companion.bar
// 2 PUTSTATIC I\$Companion.bar
// 0 INVOKESTATIC I\$Companion.access\$getBar\$p
// 0 INVOKESTATIC I\$Companion.access\$setBar\$cp
// 0 INVOKESPECIAL I\$Companion.getBar
// 0 INVOKESPECIAL I\$Companion.setBar