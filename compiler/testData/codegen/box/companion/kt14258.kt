// Checks that methods 'access$getMy$p', 'access$getMy$cp' and 'getMy' are not generated and
// that backed field 'my' is directly used through a 'getstatic'

class My {
    companion object {
        private val my: String = "OK"
    }

    fun getMyValue() = my
}

fun box() = My().getMyValue()