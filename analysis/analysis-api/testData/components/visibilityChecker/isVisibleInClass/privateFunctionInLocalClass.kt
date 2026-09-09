class Owner {
    private fun member(): Int = 42

    fun useSite(): Int {
        class <caret>Local {
            fun call(owner: Owner): Int = owner.member()
        }

        return Local().call(this)
    }
}

// callable: /Owner.member
