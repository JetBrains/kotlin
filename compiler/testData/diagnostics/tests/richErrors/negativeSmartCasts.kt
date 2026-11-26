// RUN_PIPELINE_TILL: FRONTEND
error class E1(val e1Prop: String)
error object O1

fun isNotSingleError(x: String | E1) {
    if (x is E1) return
    x.length
}

fun notEqualsSingleObject(x: String | O1) {
    if (x == O1) return
    x.length
}

fun combined(x: String | E1 | O1) {
    if (x == O1) return
    if (x is E1) return
    x.length
}

fun isNotPrimary(x: String | E1) {
    if (x is String) return
    x.e1Prop
}

fun isNotPrimaryOrError(x: String | E1 | O1) {
    if (x is String) return
    if (x == O1) return
    x.e1Prop
}

fun isNotPrimaryGeneric1(x: List<String> | E1) {
    if (x is List<String>) return
    x.e1Prop
}

fun isNotPrimaryGeneric2(x: List<String> | E1) {
    if (x is List) return
    x.e1Prop
}

fun isNotPrimaryGeneric3(x: List<String> | E1) {
    if (x is List<*>) return
    x.e1Prop
}

fun isNotPrimaryGeneric4(x: List<String> | E1) {
    if (x is Iterable<*>) return
    x.e1Prop
}

fun upperAndLowerSmartCastStable(x: Any) {
    if (x !is (String | E1)) return
    if (x is E1) return
    x.length
}

fun upperSmartCastStableLowerUnstable(x: Any) {
    var x = x
    if (x !is (String | E1)) return
    
    val closure = { x = E1("") }
    if (x is E1) return

    x<!UNSAFE_CALL!>.<!>length
}


/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, isExpression, smartcast */
