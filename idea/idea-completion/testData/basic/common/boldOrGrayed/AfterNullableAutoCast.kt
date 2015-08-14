fun String?.forNullableString(){}
fun Any?.forNullableAny(){}
fun String.forString(){}
fun Any.forAny(){}

fun foo(o: Any?) {
    if (o is String) {
        o.<caret>
    }
}

// EXIST: { lookupString: "forNullableString", attributes: "" }
// EXIST: { lookupString: "forNullableAny", attributes: "" }
// EXIST: { lookupString: "forString", attributes: "bold" }
// EXIST: { lookupString: "forAny", attributes: "" }
// EXIST: { lookupString: "compareTo", attributes: "" }
