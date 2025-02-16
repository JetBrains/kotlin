// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73608

interface MyInterface {
    fun compute()
}

data object MyObject : MyInterface {
    override fun compute() {}
}

typealias TypeAliasToObject = MyObject

fun <I> id(x: I): I = x

fun bar(x: () -> Unit) {}
fun baz(x: (MyObject) -> Unit) {}

fun main() {
    baz(<!TYPE_MISMATCH!>TypeAliasToObject::compute<!>)
    bar(TypeAliasToObject::compute)

    baz(<!TYPE_MISMATCH!>MyObject::compute<!>)
    bar(MyObject::compute)

    baz(<!TYPE_MISMATCH!>id(TypeAliasToObject::compute)<!>)
    bar(id(TypeAliasToObject::compute))

    baz(<!TYPE_MISMATCH!>id(MyObject::compute)<!>)
    bar(id(MyObject::compute))
}
