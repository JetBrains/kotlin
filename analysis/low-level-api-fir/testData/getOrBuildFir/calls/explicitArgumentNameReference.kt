// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtNameReferenceExpression

class Regular
fun named(regular: Regular) {}

fun usage() {
    named(<expr>regular</expr> = Regular())
}
