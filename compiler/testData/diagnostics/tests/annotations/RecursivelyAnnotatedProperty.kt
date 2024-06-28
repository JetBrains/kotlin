// Properties can be recursively annotated
annotation class ann(val x: Int)
class My {
    @ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>x<!>) val x: Int = 1
}
