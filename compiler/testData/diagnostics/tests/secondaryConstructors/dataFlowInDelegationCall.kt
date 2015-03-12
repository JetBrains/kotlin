// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    constructor(x: Any, y: Any, z: Any) {}
    constructor(x: String?, y: String?): this(x!!, <!DEBUG_INFO_SMARTCAST!>x<!>.length().toString() + y!!, "") {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length() + <!DEBUG_INFO_SMARTCAST!>y<!>.length()
    }
}
