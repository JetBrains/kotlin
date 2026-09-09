class <caret>Owner {
    companion object {
        private fun member(): Int = 42
    }

    fun useSite(): Int = member()
}

// callable: /Owner.Companion.member
