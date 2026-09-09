open class <caret>Owner {
    protected fun member(): Int = 42

    fun useSite(): Int = member()
}

// callable: /Owner.member
