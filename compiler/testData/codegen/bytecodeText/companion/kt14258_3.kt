// Checks that accessor methods are always used due to the overriding of the default setter of 'my' property.

class My {
    companion object {
        private var my: String = "OK"
            set(value) { field = value }
    }

    fun getMyValue(): String {
        // GETSTATIC for the companion object
        my = "Overriden value"
        // GETSTATIC for direct access to 'my' property
        return my
    }

    // PUTSTATIC for 'my' property into clinit
    // PUTSTATIC for 'my' property into 'access$getMy$cp'
}

// 2 GETSTATIC My.my
// 2 PUTSTATIC My.my
// 0 INVOKESTATIC My\$Companion.access\$getMy\$p
// 1 INVOKESTATIC My\$Companion.access\$setMy\$p
// 1 INVOKESTATIC My.access\$setMy\$cp
// 1 INVOKESTATIC My.access\$getMy\$cp
// 1 INVOKESPECIAL My\$Companion.getMy
// 1 INVOKESPECIAL My\$Companion.setMy