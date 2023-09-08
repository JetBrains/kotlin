// WITH_STDLIB
// ISSUE: KT-61691

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class FixCatchValueParameter

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.LOCAL_VARIABLE)
annotation class FixCatchLocalVariable

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class FixCatchClass

fun main() {
    try {

    } catch (@FixCatchLocalVariable @FixCatchValueParameter @FixCatchClass e: Throwable) {

    }
}
