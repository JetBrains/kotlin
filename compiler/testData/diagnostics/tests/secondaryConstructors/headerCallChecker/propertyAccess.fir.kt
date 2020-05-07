// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    val prop = 1
    constructor(x: Int)
    constructor(x: Int, y: Int, z: Int = x + prop + this.prop) :
        this(x + prop + this.prop)
}
