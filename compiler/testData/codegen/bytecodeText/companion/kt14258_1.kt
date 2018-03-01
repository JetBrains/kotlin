// Checks that methods 'access$getMy$p', 'access$getMy$cp' and 'getMy' are not generated and
// that backed field 'my' is directly used through a 'getstatic'

class My {
    companion object {
        private val my: String = "OK"
    }

    fun getMyValue() = my
}

// 1 GETSTATIC My.my
// 1 PUTSTATIC My.my
// 0 INVOKESTATIC My\$Companion.access\$getMy\$p
// 0 INVOKESTATIC My.access\$getMy\$cp
// 0 INVOKESPECIAL My\$Companion.getMy