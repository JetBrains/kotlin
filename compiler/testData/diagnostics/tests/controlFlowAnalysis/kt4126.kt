// RUN_PIPELINE_TILL: BACKEND
public data class ProductGroup(val short_name: String, val parent: ProductGroup?) {
    val name: String = if (parent == null) short_name else "${parent.name} $short_name"
}

/* GENERATED_FIR_TAGS: classDeclaration, data, equalityExpression, ifExpression, nullableType, primaryConstructor,
propertyDeclaration, smartcast, stringLiteral */
