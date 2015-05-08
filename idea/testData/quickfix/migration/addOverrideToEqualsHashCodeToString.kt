// "Add 'override' to equals, hashCode, toString in project" "true"

class A {
    fun <caret>equals(other: Any?) = false
    fun hashCode() = 0
    fun toString(): String {
        return "A"
    }
}

class B {
    open fun equals(other: Any?) = false
    open fun hashCode(): Int {
        return 42
    }
    open fun toString() = ""
}

class C {
    public fun equals(other: Any?): Boolean = true
    public fun hashCode() = 0
    public fun toString() = ""
}

class D {
    public open fun equals(o: Any?) = false
    public open fun hashCode(): Int = 239
    public open fun toString() = ""
}
