// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_VARIABLE

import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
fun <E> myBuildList(@BuilderInference builderAction: MutableList<E>.() -> Unit) {
    ArrayList<E>().builderAction()
}

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val newList1 = myBuildList {
        addAll(
            listOf(1).map { <!INAPPLICABLE_CANDIDATE!>Foo<!>(null) }
        )
    }

    val newList2 = buildList {
        addAll(listOf(1,2,3).map{ <!INAPPLICABLE_CANDIDATE!>Foo<!>(null) })
    }
}

class Foo(val notNullProp: String)
