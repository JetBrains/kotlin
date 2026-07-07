// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
abstract class ComponentPredicate : () -> Boolean {
    abstract fun addListener(listener: (Boolean) -> Unit)

    companion object {
        val TRUE: ComponentPredicate = ConstantComponentPredicate(true)
        val FALSE: ComponentPredicate = ConstantComponentPredicate(false)
    }
}

private class ConstantComponentPredicate(private val value: Boolean) : ComponentPredicate() {
    override fun addListener(listener: (Boolean) -> Unit) = Unit

    override fun invoke(): Boolean = value
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, functionalType, objectDeclaration,
operator, override, primaryConstructor, propertyDeclaration */
