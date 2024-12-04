// BODY_RESOLVE
annotation class Anno(val position: String)

fun fo<caret>o() {
    class OriginalClass {
        val prop = 0

        @Anno("property $prop")
        val <@Anno("type param $prop") F : @Anno("bound $prop") Number> @receiver:Anno("receiver annotation: $prop") @Anno("receiver type $prop") F.explicitType: @Anno("bound $prop") Int get() = 1
    }
}
