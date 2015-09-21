class MyClass

// Correct at compile time but wrong at run-time
operator fun MyClass?.inc(): MyClass? { return null }

public fun box() : MyClass? {
    var i : MyClass? 
    i = MyClass()
    var j = i++
    <!DEBUG_INFO_SMARTCAST!>j<!>.hashCode()
    return i
}
