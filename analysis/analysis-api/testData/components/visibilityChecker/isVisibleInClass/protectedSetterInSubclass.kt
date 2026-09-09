open class Owner {
    var member: Int = 42
        protected set
}

class <caret>Child : Owner() {
    fun useSite() {
        member++
    }
}

// setter: callable: /Owner.member
