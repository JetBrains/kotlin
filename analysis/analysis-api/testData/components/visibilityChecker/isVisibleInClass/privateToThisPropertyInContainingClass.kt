class <caret>Owner<in T>(value: T) {
    private val member: T = value

    fun useSite(): T = member
}

// callable: /Owner.member
