annotation(repeatable = true, retention = AnnotationRetention.SOURCE) class RepeatableAnn
annotation class Ann

public class A(@param:Ann <!REPEATED_ANNOTATION!>@Ann<!> val x: Int, @param: RepeatableAnn @Ann val y: Int) {

    @field:Ann @property:Ann @RepeatableAnn @property:RepeatableAnn
    val a: Int = 0

    @Ann <!REPEATED_ANNOTATION!>@field:Ann<!> <!REPEATED_ANNOTATION!>@property:Ann<!>
    val b: Int = 0

    @field:RepeatableAnn @field:RepeatableAnn
    val c: Int = 0

    @property:RepeatableAnn @RepeatableAnn
    val d: Int = 0

}