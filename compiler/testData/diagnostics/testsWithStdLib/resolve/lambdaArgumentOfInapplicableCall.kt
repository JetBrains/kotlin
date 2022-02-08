// KT-45010
fun foo(map: MutableMap<Int, String>) {
    map.getOrPut(<!TYPE_MISMATCH!>"Not an Int"<!>) {
        "Hello" + " world"
    }
}
