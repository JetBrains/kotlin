class Owner {
    internal fun member(): Int = 42
}

class <caret>Unrelated {
    fun useSite(owner: Owner): Int = owner.member()
}

// callable: /Owner.member
