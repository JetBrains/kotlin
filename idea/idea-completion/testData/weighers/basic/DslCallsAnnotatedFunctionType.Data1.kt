package bar

@DslMarker
annotation class Dsl


class R

fun r(body: @Dsl R.() -> Unit) {

}

fun foo1(i: Int) {

}

fun foo3() {

}

@Dsl
fun R.foo2() {}

@Dsl
fun R.foo4() {

}

@Dsl
fun R.fooloooooong() {

}

val R.fooval
    get() = Unit

@Dsl
fun R.SomethingSomethingFooSomething() {

}

