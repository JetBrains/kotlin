open class Owner {
    private fun member(): Int = 42

    class <caret>Nested : Owner() {
        fun useSite(): Int = member()
    }
}

// callable: /Owner.member
