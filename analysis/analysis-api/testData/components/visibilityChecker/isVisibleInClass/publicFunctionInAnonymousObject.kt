class Owner {
    fun member(): Int = 42
}

val useSite = <caret>object {
    fun call(owner: Owner): Int = owner.member()
}

// callable: /Owner.member
