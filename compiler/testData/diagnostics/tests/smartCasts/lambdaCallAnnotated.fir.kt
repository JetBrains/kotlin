// See KT-9134: smart cast is not provided inside lambda call

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class My

fun bar(): Int = @My <!UNRESOLVED_REFERENCE!>{
    var i: Int?
    i = 42
    i
}()<!>