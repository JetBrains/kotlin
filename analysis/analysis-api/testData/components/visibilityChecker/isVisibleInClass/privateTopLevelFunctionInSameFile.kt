private fun helper(): Int = 42

class <caret>Owner {
    fun useSite(): Int = helper()
}

// callable: /helper
