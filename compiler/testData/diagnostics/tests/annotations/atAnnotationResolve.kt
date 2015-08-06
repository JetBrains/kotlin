// !DIAGNOSTICS: -UNUSED_PARAMETER
target(AnnotationTarget.TYPE, AnnotationTarget.CLASSIFIER,
       AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION,
       AnnotationTarget.EXPRESSION, AnnotationTarget.PROPERTY)
annotation(repeatable = true, retention = AnnotationRetention.SOURCE) class Ann(val x: Int = 6)

@Ann(1) @Ann(2) @Ann(3) @private class A @Ann constructor() {
    @Ann(x = 5) fun foo() {
        1 + @Ann(1) 1 * @Ann(<!TYPE_MISMATCH!>""<!>) 6

        @Ann fun local() {}
    }

    @Ann val x = 1

    fun bar(x: @Ann(1) @Ann(2) @Ann(3) Int) {}
}
