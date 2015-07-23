annotation(repeatable = true) class repann

annotation(retention = AnnotationRetention.SOURCE, repeatable = true) class repann1(val x: Int)

annotation(repeatable = true, retention = AnnotationRetention.SOURCE) class repann2(val f: Boolean)

target(AnnotationTarget.EXPRESSION) annotation(repeatable = true) class repexpr

repann repann class DoubleAnnotated

repann1(1) repann1(2) repann1(3) class TripleAnnotated

repann2(true) repann2(false) repann2(false) repann2(true) class FourTimesAnnotated

@repann @repann fun foo(@repann @repann x: Int): Int {
    @repexpr @repexpr return x
}