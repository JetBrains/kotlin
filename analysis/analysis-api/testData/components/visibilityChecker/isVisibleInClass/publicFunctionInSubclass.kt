open class Owner {
    fun member(): Int = 42
}

class <caret>Child : Owner()

// callable: /Owner.member
