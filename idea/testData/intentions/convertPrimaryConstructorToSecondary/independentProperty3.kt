fun foo(x: Int) {
    class Local<caret>(val y: Int) {
        val z = x
    }
}