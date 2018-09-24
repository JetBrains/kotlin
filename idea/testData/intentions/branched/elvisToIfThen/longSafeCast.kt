class My(val x: Int)
fun foo(arg: Any) {
    val y = (arg as? My)?.x?.hashCode() <caret>?: 42
}