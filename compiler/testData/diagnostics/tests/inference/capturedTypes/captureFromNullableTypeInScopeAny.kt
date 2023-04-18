// FIR_IDENTICAL
// ISSUE: KT-57958

fun ListVM<*>.foo() {
    val currentItem1: MutableProperty<out Any?> = <!DEBUG_INFO_EXPRESSION_TYPE("MutableProperty<in kotlin.Nothing?>")!>currentItem<!>
}

interface MutableProperty<T> {
    var value: T
}

interface ListVM<TItemVM> {
    val currentItem: MutableProperty<TItemVM?>
}
