class MyClass

operator fun MyClass?.inc(): MyClass? = null

public fun box() : String {
    var i : MyClass? 
    i = MyClass()
    val j = ++i

    return if (j == null && null == i) "OK" else "fail i = $i j = $j"
}
