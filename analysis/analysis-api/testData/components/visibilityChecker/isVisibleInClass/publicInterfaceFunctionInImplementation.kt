interface Owner {
    fun member(): Int = 42
}

class <caret>Implementation : Owner

// callable: /Owner.member
