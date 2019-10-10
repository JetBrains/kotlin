open class ParentClass() {
    val x: Int
    constructor(val x: Int) {
        this.x = x
    }
}

class ChildClass : ParentClass(9)

fun box(): String {
    val parentClass = ParentClass(9)
    val childClass = ChildClass()
    if (parentClass.x == childClass.x){
        return "OK"
    } else {
        return "FAIL"
    }
}