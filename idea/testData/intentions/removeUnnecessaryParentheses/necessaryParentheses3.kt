// IS_APPLICABLE: false
fun foo(x: Int) : Any {
    return <caret>(x as Int) < 42
}