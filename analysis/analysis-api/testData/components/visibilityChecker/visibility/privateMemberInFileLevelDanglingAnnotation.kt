annotation class Anno(val value: Int)

class Owner {
    private fun <caret_target>target(): Int = 0
}

@Anno(p<caret>rintln())
