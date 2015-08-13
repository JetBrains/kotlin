trait T {
    fun f(){}
}

fun foo(o: T) {
    if (o is Runnable) {
        o.<caret>
    }
}

// EXIST: { itemText: "run", attributes: "bold" }
// EXIST: { itemText: "f", attributes: "bold" }
// EXIST: { itemText: "hashCode", attributes: "" }
// EXIST: { itemText: "equals", attributes: "" }
