// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class Example

fun Example.plus(other: Example) = 0
operator infix fun Example.minus(other: Example) = 0

operator infix fun Example.times(other: Example) = 0
fun Example.div(other: Example) = 0

fun a() {
    <!CANNOT_INFER_PARAMETER_TYPE!>with<!> (Example()) {
        operator infix fun Example.plus(other: Example) = ""
        fun Example.minus(other: Example) = ""

        operator infix fun Example.times(other: Example) = ""
        fun Example.div(other: Example) = ""

        <!CANNOT_INFER_PARAMETER_TYPE!>with<!> (Example()) {
            val a = Example()
            val b = Example()

            consumeString(a + b)
            consumeInt(a - b)

            consumeString(a plus b)
            consumeInt(a minus b)

            a * b
            a <!NONE_APPLICABLE!>/<!> b

            a times b
            a <!NONE_APPLICABLE!>div<!> b
        }
    }
}

fun consumeInt(i: Int) {}
fun consumeString(s: String) {}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration, infix,
integerLiteral, lambdaLiteral, localFunction, localProperty, multiplicativeExpression, operator, propertyDeclaration,
stringLiteral */
