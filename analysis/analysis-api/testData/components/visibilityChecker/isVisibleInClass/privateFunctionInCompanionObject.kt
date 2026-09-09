class Owner {
    private fun member(): Int = 42

    companion <caret>object {
        fun useSite(owner: Owner): Int = owner.member()
    }
}

// callable: /Owner.member
