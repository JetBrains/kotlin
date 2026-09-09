open class Owner {
    protected fun member(): Int = 42
}

open class Child : Owner()

class <caret>GrandChild : Child() {
    fun useSite(): Int = member()
}

// callable: /Owner.member
