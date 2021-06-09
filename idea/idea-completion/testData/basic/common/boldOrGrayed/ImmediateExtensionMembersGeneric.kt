// FIR_COMPARISON
fun <T> List<T>.forListT(){}
fun <T> Collection<T>.forCollectionT(){}
fun <T> T.forT() {}

fun foo(list: List<String>) {
    list.<caret>
}

// EXIST: { itemText: "forListT", attributes: "bold" }
// EXIST: { itemText: "forCollectionT", attributes: "" }
// EXIST: { itemText: "forT", attributes: "" }
