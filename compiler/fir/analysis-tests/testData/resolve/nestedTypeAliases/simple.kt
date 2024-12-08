// RUN_PIPELINE_TILL: BACKEND

class Foo<T> {
    inner class Inner
}

typealias InnerAlias<K> = Foo<K>.Inner

fun test() {
    val foo = Foo<String>()

    foo.InnerAlias()
//    foo.Inner()
/*    val aliasedInner = Foo<String>::InnerAlias
    aliasedInner(foo)*/
}