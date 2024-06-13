// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn
// DIAGNOSTICS: -UNUSED_VARIABLE

import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
fun <E> myBuildList(builderAction: MutableList<E>.() -> Unit) {
    ArrayList<E>().builderAction()
}

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val newList1 = myBuildList {
        addAll(
            listOf(1).map { Foo(<!NULL_FOR_NONNULL_TYPE!>null<!>) }
        )
    }

    val newList2 = buildList {
        addAll(listOf(1,2,3).map{ Foo(<!NULL_FOR_NONNULL_TYPE!>null<!>) })
    }
}

class Foo(val notNullProp: String)
