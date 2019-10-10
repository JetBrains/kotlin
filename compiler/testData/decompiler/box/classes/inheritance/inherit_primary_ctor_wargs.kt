open class ParentClass(val x: Int) {
    val anotherX = x
}

class ChildClass : ParentClass(9)

fun box(): String {
    val parentClass = ParentClass(9)
    val childClass = ChildClass()
    if (parentClass.anotherX == childClass.anotherX){
        return "OK"
    } else {
        return "FAIL"
    }
}