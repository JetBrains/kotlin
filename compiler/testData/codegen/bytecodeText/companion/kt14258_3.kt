// IGNORE_BACKEND: JVM_IR
// Checks that accessor methods are always used due to the overriding of the default setter of 'my' property.

class My {
    companion object {
        private var my: String = "OK"
            set(value) { field = value }
    }

    fun getMyValue(): String {
        // INVOKESTATIC My$Companion.access$setMy$p
        my = "Overriden value"
        // GETSTATIC My.my
        return my
    }

    // PUTSTATIC My.my into clinit
    // PUTSTATIC My.my into 'access$setMy$cp'
    // GETSTATIC My.my into 'access$getMy$cp'
}

// 2 GETSTATIC My.my
// 2 PUTSTATIC My.my
// 0 INVOKESTATIC My\$Companion.access\$getMy\$p
// 1 INVOKESTATIC My\$Companion.access\$setMy\$p
// 1 INVOKESTATIC My.access\$setMy\$cp
// 1 INVOKESTATIC My.access\$getMy\$cp
// 1 INVOKESPECIAL My\$Companion.getMy
// 1 INVOKESPECIAL My\$Companion.setMy