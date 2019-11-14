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

// 2 PUTSTATIC My.my
// 0 INVOKESTATIC My\$Companion.access\$getMy\$p
// 1 INVOKESTATIC My.access\$setMy\$cp
// 1 INVOKESPECIAL My\$Companion.setMy

// JVM_TEMPLATES
// 2 GETSTATIC My.my
// 1 INVOKESTATIC My\$Companion.access\$setMy\$p
// 1 INVOKESTATIC My.access\$getMy\$cp
// 1 INVOKESPECIAL My\$Companion.getMy

// IR only generates the accessors actually needed
// JVM_IR_TEMPLATES
// 1 GETSTATIC My.my
