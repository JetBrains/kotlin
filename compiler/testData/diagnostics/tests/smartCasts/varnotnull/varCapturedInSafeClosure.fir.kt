// RUN_PIPELINE_TILL: BACKEND
public fun foo() {
    var s: String? = ""
    fun closure(): Int {
        if (s == null) {
            return -1
        } else {
            return 0
        }
    }
    if (s != null) {
        System.out.println(closure())
        // Smart cast is possible, nobody modifies s
        System.out.println(s.length)
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, flexibleType, functionDeclaration, ifExpression, integerLiteral, javaFunction,
javaProperty, localFunction, localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral */
