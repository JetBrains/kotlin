// !WITH_NEW_INFERENCE

fun <T> materialize(): T = TODO()

fun implicitCoercion() {
    val <!UNUSED_VARIABLE!>a<!> = {
        // Block is implicitly Unit-coerced, so it is allowed to place statement at the end of lambda
        if (true) <!UNUSED_EXPRESSION!>42<!>
    }

    val <!UNUSED_VARIABLE!>b<!> = l@{
        return@l
    }

    val <!UNUSED_VARIABLE!>c<!> = l@{
        // Error: block doesn't have an expected type, so call can't be inferred!
        <!NI;UNREACHABLE_CODE!>return@l<!> <!NI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>()
    }
}