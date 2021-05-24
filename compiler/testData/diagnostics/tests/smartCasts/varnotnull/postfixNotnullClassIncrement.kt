class MyClass

operator fun MyClass.inc(): MyClass { return null!! }

public fun box() : MyClass? {
    var i : MyClass?
    i = MyClass()
    // type of j can be inferred as MyClass()
    var j = <!DEBUG_INFO_SMARTCAST!>i<!>++
    <!DEBUG_INFO_SMARTCAST!>j<!>.hashCode()
    return i
}
