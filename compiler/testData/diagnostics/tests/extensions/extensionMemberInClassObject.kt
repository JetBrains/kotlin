// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

interface JPAEntityClass<D> {
    fun <T> T.findByName(s: String): D {null!!}
}

class Foo {
    companion object : JPAEntityClass<Foo>
}

fun main() {
    with("", {
        Foo.<!UNRESOLVED_REFERENCE!>findByName<!>("")
    })
    with(Foo) {
        findByName("")
    }
}
