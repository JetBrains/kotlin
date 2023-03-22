// IGNORE_REVERSED_RESOLVE
annotation class base

@base class My {
    <!WRONG_ANNOTATION_TARGET!>@base<!> init {
    }
}
