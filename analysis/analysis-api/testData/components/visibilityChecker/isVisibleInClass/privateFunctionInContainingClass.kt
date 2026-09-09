class <caret>Owner {
    private fun member(): Int = 42

    fun useSite(): Int = member()
}

// callable: /Owner.member
