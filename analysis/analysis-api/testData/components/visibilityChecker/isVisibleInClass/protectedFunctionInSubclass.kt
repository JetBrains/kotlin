open class Owner {
    protected fun member(): Int = 42
}

class <caret>Child : Owner() {
    fun useSite(): Int = member()
}

// callable: /Owner.member
