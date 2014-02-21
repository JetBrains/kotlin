// IS_APPLICABLE: false
fun foo(y: Int = 0, z: (Int) -> String = {""}) {
    foo {
        4<caret>: Int
        ""
    }
}
