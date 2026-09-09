class Owner {
    private fun member(): Int = 42

    class <caret>Nested {
        fun useSite(owner: Owner): Int = owner.member()
    }
}

// callable: /Owner.member
