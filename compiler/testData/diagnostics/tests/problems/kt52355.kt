// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-52355
// WITH_STDLIB

// KT-52355: Not correct specified type parameter in the error OVERLOAD_RESOLUTION_AMBIGUITY_BECAUSE_OF_STUB_TYPES

class ItemHolder<T> {
    private val items = mutableListOf<T>()

    fun addItem(x: T) {
        items.add(x)
    }

    fun getLastItem(): T? = items.lastOrNull()
}

fun <T> ItemHolder<T>.addAllItems(xs: List<T>) {
    xs.forEach { addItem(it) }
}

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
fun <T> itemHolderBuilder(@<!DEPRECATION!>BuilderInference<!> builder: ItemHolder<T>.() -> Unit): ItemHolder<T> =
    ItemHolder<T>().apply(builder)

fun test(s: Int) {
    val itemHolder3 = itemHolderBuilder {
        addItem(s)
        val lastItem = getLastItem()
        println(lastItem)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, funWithExtensionReceiver, functionDeclaration, functionalType,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, typeParameter, typeWithExtension */
