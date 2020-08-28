// ISSUE: KT-39074

interface A
interface B : A {
    fun bar()
}

fun <K : A> materialize(): K? = null!!

fun foo(b: B, cond: Boolean) {
    val x = // inferred as A
        if (cond)
            b
        else
            materialize() ?: return

    x.bar()
}
