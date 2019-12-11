// !WITH_NEW_INFERENCE
class MyClass

operator fun MyClass.inc(): MyClass { return null!! }

public fun box() : MyClass? {
    var i : MyClass?
    i = MyClass()
    // type of j can be inferred as MyClass()
    var j = i++
    j.hashCode()
    return i
}