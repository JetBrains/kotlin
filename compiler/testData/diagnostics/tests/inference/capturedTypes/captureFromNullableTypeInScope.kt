// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-57958

fun ListVM<*>.foo() {
    val currentItem1: MutableProperty<out ListItemVM<*>?> = currentItem
}

interface MutableProperty<T> {
    var value: T
}

interface ListItemVM<out TItem> {
    val value: TItem
}

interface ListVM<TItemVM : ListItemVM<*>> {
    val currentItem: MutableProperty<TItemVM?>
}

/* GENERATED_FIR_TAGS: capturedType, funWithExtensionReceiver, functionDeclaration, interfaceDeclaration, localProperty,
nullableType, out, outProjection, propertyDeclaration, starProjection, typeConstraint, typeParameter */
