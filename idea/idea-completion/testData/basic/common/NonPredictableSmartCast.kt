fun String.forString(){}
fun Any.forAny(){}

fun <T> T.forT() {}

fun f(pair: Pair<out Any, out Any>) {
    if (pair.first !is String) return
    pair.first.<caret>
}

// EXIST: { lookupString: "length", attributes: "grayed" }
// EXIST: { lookupString: "hashCode", attributes: "bold" }
// EXIST: { lookupString: "forString", attributes: "grayed" }
// EXIST: { lookupString: "forAny", attributes: "bold" }
// EXIST: { lookupString: "forT", attributes: "bold" }
