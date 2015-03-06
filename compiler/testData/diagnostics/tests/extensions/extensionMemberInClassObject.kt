// !DIAGNOSTICS: -UNUSED_PARAMETER

trait JPAEntityClass<D> {
    fun <T> T.findByName(s: String): D {null!!}
}

class Foo {
    default object : JPAEntityClass<Foo>
}

fun main(args: Array<String>) {
    <!TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH!>with<!>("", <!TYPE_MISMATCH!>{
        Foo.<!MISSING_RECEIVER, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>findByName<!>("")
    }<!>)
}

fun <T> with(t: T, f: T.() -> Unit) {}