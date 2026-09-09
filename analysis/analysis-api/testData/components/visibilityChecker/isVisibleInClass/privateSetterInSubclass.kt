open class Owner {
    var member: Int = 42
        private set
}

class <caret>Child : Owner()

// setter: callable: /Owner.member
