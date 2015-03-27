// !DIAGNOSTICS: -UNUSED_PARAMETER
class Outer {
    val prop = 1
    inner class Inner {
        constructor(x: Int)
        constructor(x: Int, y: Int, z: Int = x + prop + this@Outer.prop) : this(x + prop + this@Outer.prop)
    }
}
