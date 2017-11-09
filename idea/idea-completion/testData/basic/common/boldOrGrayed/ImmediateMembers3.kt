interface T {
    fun f(){}
}

fun foo(o: T) {
    if (o is Comparable<T>) {
        o.<caret>
    }
}

// EXIST: { itemText: "compareTo", attributes: "bold" }
// EXIST: { itemText: "f", attributes: "bold" }
// EXIST: { itemText: "hashCode", attributes: "" }
// EXIST: { itemText: "equals", attributes: "" }
