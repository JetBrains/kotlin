// Checks that methods 'access$getMy$p' and 'getMy' are not generated and
// that backed field 'my' is accessed through 'access$getMy$cp'

class My {
    companion object {
        private val my: String = "OK"

        fun test(): String {
            // accessor is required because field is move to Foo
            return my
        }
    }

    fun getMyValue() = test()
}

// 1 GETSTATIC My.my
// 0 INVOKESTATIC My\$Companion.access\$getMy\$p
// 1 INVOKESTATIC My.access\$getMy\$cp
// 0 INVOKESPECIAL My\$Companion.getMy