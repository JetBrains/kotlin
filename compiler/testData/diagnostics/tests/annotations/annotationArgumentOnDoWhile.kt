// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76931

const val myConst = 1

fun main() {
    @Anno(myConst)
    do {
        val myConst = 2
    } while (false)
}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Anno(val value: Int)
