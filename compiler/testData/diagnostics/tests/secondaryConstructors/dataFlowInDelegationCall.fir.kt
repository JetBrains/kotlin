// DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    constructor(x: Any, y: Any, z: Any)
    constructor(x: String?, y: String?): this(x!!, x.length.toString() + y!!, "") {
        x.length + y.length
    }
}
