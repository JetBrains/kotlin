// !DIAGNOSTICS: -UNUSED_PARAMETER

interface JPAEntityClass<D> {
    fun <T> T.findByName(s: String): D {null!!}
}

class Foo {
    companion object : JPAEntityClass<Foo>
}

fun main(args: Array<String>) {
    <!TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>with<!>("", <!TYPE_MISMATCH!>{
        Foo.<!MISSING_RECEIVER, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>findByName<!>("")
    }<!>)
}

fun <T> with(t: T, f: T.() -> Unit) {}