// WITH_STDLIB
const val integer = 0

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Anno(val number: Int)

fun usage() {
    @Anno(0 + integer) (@Anno(integer) usage()).toString()
}
