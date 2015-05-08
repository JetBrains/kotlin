// "Add 'override' to equals, hashCode, toString in project" "true"

class A {
    override fun equals(other: Any?) = false
    override fun hashCode() = 0
    override fun toString(): String {
        return "A"
    }
}

class B {
    override fun equals(other: Any?) = false
    override fun hashCode(): Int {
        return 42
    }
    override fun toString() = ""
}

class C {
    override fun equals(other: Any?): Boolean = true
    override fun hashCode() = 0
    override fun toString() = ""
}

class D {
    override fun equals(o: Any?) = false
    override fun hashCode(): Int = 239
    override fun toString() = ""
}
