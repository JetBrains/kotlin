fun String?.forNullableString(){}
fun Any?.forNullableAny(){}
fun String.forString(){}
fun Any.forAny(){}

fun foo(s: String?) {
    s.<caret>
}

// EXIST: { lookupString: "forNullableString", attributes: "bold" }
// EXIST: { lookupString: "forNullableAny", attributes: "" }
// EXIST: { lookupString: "forString", attributes: "grayed" }
// EXIST: { lookupString: "forAny", attributes: "grayed" }
// EXIST: { lookupString: "compareTo", attributes: "grayed" }
