// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_VARIABLE

class Item(val link: String?)

fun test(item: Item) {
    if (item.link != null) {
        val href: String = item.link
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, ifExpression, localProperty,
nullableType, primaryConstructor, propertyDeclaration, smartcast */
