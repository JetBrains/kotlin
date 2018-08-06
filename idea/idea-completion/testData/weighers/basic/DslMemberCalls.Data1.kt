package bar

@DslMarker
annotation class Dsl


@Dsl
class R {
    fun foo1(i: Int) {

    }

    @Dsl
    fun foo2(i : Int) {

    }

    val foo5: Int = 6
    @Dsl
    val foo6: Int = 7


}

fun r(body: R.() -> Unit) {

}


@Dsl
fun R.foo4() {

}

@Dsl
fun R.foo3() {

}