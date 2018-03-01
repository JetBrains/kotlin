// Checks that accessor 'I$Companion.access$getBar\$p' is always used because the property is kept
// into the companion object.

interface I {
    companion object {
        private val bar = "Companion Field from I"
    }

    fun test(): String {
        // accessor is required because field is kept into companion object
        return bar
    }
}

// 1 GETSTATIC I\$Companion.bar
// 1 PUTSTATIC I\$Companion.bar
// 1 INVOKESTATIC I\$Companion.access\$getBar\$p
// 0 INVOKESTATIC I\$Companion.access\$setBar\$cp
// 0 INVOKESPECIAL I\$Companion.getBar
// 0 INVOKESPECIAL I\$Companion.setBar