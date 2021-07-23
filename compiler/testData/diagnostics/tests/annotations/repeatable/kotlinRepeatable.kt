@Repeatable
annotation class repann

@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class repann1(val x: Int)

@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class repann2(val f: Boolean)

@Retention(AnnotationRetention.BINARY)
@Repeatable
annotation class binrepann

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class repexpr

@repann <!NON_SOURCE_REPEATED_ANNOTATION!>@repann<!> class DoubleAnnotated

@repann1(1) @repann1(2) @repann1(3) class TripleAnnotated

@repann2(true) @repann2(false) @repann2(false) @repann2(true) class FourTimesAnnotated

@binrepann <!NON_SOURCE_REPEATED_ANNOTATION!>@binrepann<!> class BinaryAnnotated

@repann <!NON_SOURCE_REPEATED_ANNOTATION!>@repann<!> fun foo(@repann <!NON_SOURCE_REPEATED_ANNOTATION!>@repann<!> x: Int): Int {
    @repexpr @repexpr return x
}