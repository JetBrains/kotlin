class <caret>Owner {
    var member: Int = 42
        private set

    fun useSite() {
        member++
    }
}

// setter: callable: /Owner.member
