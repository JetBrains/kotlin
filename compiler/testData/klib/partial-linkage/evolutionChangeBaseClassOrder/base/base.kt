open class X {
    open val bar: String 
        get() = "base class open"
    val zon: String 
        get() = "base class"
}

interface Y {
    val qux: String 
        get() = "base interface Y"
}

interface Z {
    val sep: String 
        get() = "base interface Z"
}

