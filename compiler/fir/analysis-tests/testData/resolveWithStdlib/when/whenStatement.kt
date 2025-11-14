// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun testStatementSyntax() {
    val deliveryStatus = "OutForDelivery"

    when(deliveryStatus) {
        "Pending" -> print("Your order is being prepared")
        "Shipped" -> print("Your order is on the way")
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, propertyDeclaration, stringLiteral, whenExpression */
