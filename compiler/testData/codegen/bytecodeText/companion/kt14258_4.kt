// IGNORE_BACKEND: JVM_IR
// Checks that accessor 'I$Companion.access$getBar\$p' is always used because the property is kept
// into the companion object.

interface I {
    companion object {
        private var bar = "Companion Field from I"
    }

    fun test(): String {
        // INVOKESTATIC I$Companion.access$setBar$p
        bar = "New value"
        // INVOKESTATIC I$Companion.access$getBar$p
        return bar
    }
}

// 1 GETSTATIC I\$Companion.bar
// 2 PUTSTATIC I\$Companion.bar
// 1 INVOKESTATIC I\$Companion.access\$getBar\$p
// 1 INVOKESTATIC I\$Companion.access\$setBar\$p
// 0 INVOKESTATIC I\$Companion.access\$setBar\$cp
// 0 INVOKESPECIAL I\$Companion.getBar
// 0 INVOKESPECIAL I\$Companion.setBar