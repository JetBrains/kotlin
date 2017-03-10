class Owner(val z: Int) {
    fun foo(y: Int) = y + z
    val x = <caret>this::foo
}