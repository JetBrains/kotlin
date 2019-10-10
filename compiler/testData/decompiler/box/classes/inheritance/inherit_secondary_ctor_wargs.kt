open class ParentClass() {
    var x: Int = 5

    constructor(x: Int) : this() {
        this.x = x
    }
}

class ChildClass() : ParentClass(9) {
    constructor(y: Int) : this() {
    }
}

fun box(): String {
    val parentClass = ParentClass(9)
    val childClass = ChildClass()
    if (parentClass.x == childClass.x) {
        return "OK"
    } else {
        return "FAIL"
    }
}