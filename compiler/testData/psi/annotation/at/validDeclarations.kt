private @open @[Ann1(1)] @Ann2("1") @Ann3("2") class A(
        @Volatile(1) private val x: @AnnType("3") @open Int,
        @private var y: Int,
        @open z: Int
) {
    @private @[Ann3(2)] @Ann4("4") fun foo() {
        @data class LocalClass

        print(1)

        @inline(option1, option2)

        @[inline2] private
        fun inlineLocal() {}

        @[Ann]
        private
        @abstract
        @Volatile var x = 1

        foo(fun(@vararg @ann(1) x: Int) {})
    }

    val x: Int
        @inject @[inline] private @open get() = 1

    @open @ann init {}

    @companion object

    @companion @private object B;

    @main

    @private
    constructor()

    fun <@ann("") @[ann] T : R> foo() {}
}
@private val x = 1

@inline private fun bar() = 1

fun bar() {
    try {}
    catch (@Volatile e: Exception) {}
}
