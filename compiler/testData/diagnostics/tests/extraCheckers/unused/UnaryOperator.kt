// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75695

fun use(x: Any) {}

fun testIncrementPrefix() {
    var x = 1
    use(++x)
}

fun testIncrementPostfix() {
    var x = 1
    use(<!ASSIGNED_VALUE_IS_NEVER_READ!>x<!>++)
}

fun testDecrementPrefix() {
    var x = 1
    use(--x)
}

fun testDecrementPostfix() {
    var x = 1
    use(<!ASSIGNED_VALUE_IS_NEVER_READ!>x<!>--)
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, incrementDecrementExpression, integerLiteral, localProperty,
propertyDeclaration */
