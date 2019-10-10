open class ParentClass()

class ChildClass : ParentClass()

fun box(): String {
    val parentClass = ParentClass()
    val childClass = ChildClass()
    return "OK"
}