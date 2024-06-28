// Properties can be recursively annotated
annotation class ann(val x: Int)
class My {
    @ann(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>x<!>) val x: Int = 1
}
