// RUN_PIPELINE_TILL: BACKEND
class DropDownComponent<T : Any>(val initialValues: List<T>)

fun test(strings: List<String>) {
    val dropDown = DropDownComponent(
        initialValues = buildList {
            addAll(strings)
        }
    )
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, lambdaLiteral, localProperty, primaryConstructor,
propertyDeclaration, typeConstraint, typeParameter */
