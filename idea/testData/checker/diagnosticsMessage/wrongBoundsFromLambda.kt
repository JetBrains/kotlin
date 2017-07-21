fun <T> foo(<warning>x</warning>: T, <warning>l</warning>: (T) -> Unit) {}

fun testWrongParameterTypeOfLambda() {
    foo("", <error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
should be a subtype of: Byte? (for parameter 'l')
should be a supertype of: String (for parameter 'x')">{ x: Byte? -> }</error>)
}

fun <T : Number> fooReturn(<warning>x</warning>: T, <warning>l</warning>: () -> T) {}

fun myTest() {
    fooReturn(1) {
        val someExpr = ""
        <error descr="[CONTRADICTION_FOR_SPECIAL_CALL] Result type for 'if' expression cannot be inferred:
should be conformed to: Number
should be a supertype of: String (for parameter 'thenBranch'), {Int & Byte & Short & Long} (for parameter 'elseBranch')">if (true) someExpr else 2</error>
    }
}

fun testLambdaLastExpression() {
    fooReturn(1) {
        val longLongLambda = ""
        <error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
should be a subtype of: Number (declared upper bound T)
should be a supertype of: {Int & Byte & Short & Long} (for parameter 'x'), String">longLongLambda</error>
    }

    fooReturn(<error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
should be a subtype of: Number (declared upper bound T)
should be a supertype of: String (for parameter 'x'), Int">""</error>) {
    val long = 3
    long
}
}

fun <T : Number> onlyLambda(<warning>x</warning>: () -> T) {}

fun testOnlyLambda() {
    onlyLambda {
        val longLong = 123
        <error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
should be a subtype of: Number (declared upper bound T)
should be a supertype of: String">longLong.toString()</error>
    }
}

fun testLambdaWithReturnIfExpression(): Int {
    return <error descr="[TYPE_MISMATCH] Type mismatch: inferred type is Unit but Int was expected">onlyLambda {
        if (3 > 2) {
            return@onlyLambda <error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
should be a subtype of: Number (declared upper bound T)
should be a supertype of: String, {Int & Byte & Short & Long}">"not a number"</error>
        }
        if (3 < 2) {
            <error descr="[RETURN_NOT_ALLOWED] 'return' is not allowed here">return</error> <error descr="[TYPE_MISMATCH] Type mismatch: inferred type is String but Int was expected">"also not an int"</error>
        }
        <error descr="[CONTRADICTION_FOR_SPECIAL_CALL] Result type for 'if' expression cannot be inferred:
should be conformed to: Number
should be a supertype of: String (for parameter 'thenBranch'), {Int & Byte & Short & Long} (for parameter 'elseBranch')">if (true) "" else 123</error>
    }</error>
}