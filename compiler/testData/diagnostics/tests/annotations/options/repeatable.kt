annotation(repeatable = true) class repann

annotation(retention = AnnotationRetention.SOURCE, repeatable = true) class repann1(val x: Int)

annotation(repeatable = true, retention = AnnotationRetention.SOURCE) class repann2(val f: Boolean)

annotation(repeatable = true, retention = AnnotationRetention.BINARY) class binrepann

target(AnnotationTarget.EXPRESSION) annotation(repeatable = true) class repexpr

repann <!NON_SOURCE_REPEATED_ANNOTATION!>repann<!> class DoubleAnnotated

repann1(1) repann1(2) repann1(3) class TripleAnnotated

repann2(true) repann2(false) repann2(false) repann2(true) class FourTimesAnnotated

binrepann <!NON_SOURCE_REPEATED_ANNOTATION!>binrepann<!> class BinaryAnnotated

@repann <!NON_SOURCE_REPEATED_ANNOTATION!>@repann<!> fun foo(@repann <!NON_SOURCE_REPEATED_ANNOTATION!>@repann<!> x: Int): Int {
    @repexpr <!NON_SOURCE_REPEATED_ANNOTATION!>@repexpr<!> return x
}