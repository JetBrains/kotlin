annotation class AnotherAnnotation
enum class EnumClass {
    First, Second
}

annotation class Anno(val str: String, val ann: AnotherAnnotation, val c: KClass<*>, val entry: EnumClass)

@Deprecated(boo()) @Anno("123", AnotherAnnotation(), AnotherAnnotation::class, EnumClass.Second)
fun f<caret>oo() {

}