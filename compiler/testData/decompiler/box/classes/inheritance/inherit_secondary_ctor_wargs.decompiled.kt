open class ParentClass() {
    var x: Int = 5
    constructor(x: Int) : ParentClass() {
        this.x = x
    }
}
class ChildClass() : ParentClass(9) {
}
fun box(): String  {
    val parentClass: ParentClass = ParentClass(9)
    val childClass: ChildClass = ChildClass()
    if (parentClass.x == childClass.x) {
        return "OK"
    }
    else {
        return "FAIL"
    }
}
