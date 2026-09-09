class Owner<in T>(value: T) {
    private val member: T = value
}

class <caret>Unrelated

// callable: /Owner.member
