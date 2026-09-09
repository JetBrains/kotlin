class Owner {
    fun member(): Int = 42
}

object <caret>Holder {
    fun call(owner: Owner): Int = owner.member()
}

// callable: /Owner.member
