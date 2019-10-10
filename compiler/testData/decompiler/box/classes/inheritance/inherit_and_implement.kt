open class ParentClass(val x: Int)

interface MyInterface

class ChildClass : ParentClass(9), MyInterface

fun box(): String {
    val parentClass = ParentClass(9)
    val childClass = ChildClass()
    if (parentClass.x == childClass.x){
        return "OK"
    } else {
        return "FAIL"
    }
}