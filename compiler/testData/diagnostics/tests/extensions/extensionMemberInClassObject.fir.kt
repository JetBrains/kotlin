// !DIAGNOSTICS: -UNUSED_PARAMETER

interface JPAEntityClass<D> {
    fun <T> T.findByName(s: String): D {null!!}
}

class Foo {
    companion object : JPAEntityClass<Foo>
}

fun main() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>("", {
        Foo.<!UNRESOLVED_REFERENCE!>findByName<!>("")
    })
    with(Foo) {
        findByName("")
    }
}
