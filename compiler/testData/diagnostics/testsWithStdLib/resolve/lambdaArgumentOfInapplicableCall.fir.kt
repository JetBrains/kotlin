// KT-45010
fun foo(map: MutableMap<Int, String>) {
    map.<!INAPPLICABLE_CANDIDATE!>getOrPut<!>("Not an Int") {
        "Hello" + " world"
    }
}
