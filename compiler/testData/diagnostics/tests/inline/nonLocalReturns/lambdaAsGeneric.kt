// !WITH_NEW_INFERENCE

fun box() : String {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}!>test<!> {
        <!RETURN_NOT_ALLOWED!>return@box<!> "123"
    }

    return "OK"
}

<!NOTHING_TO_INLINE!>inline<!> fun <T> test(p: T) {
    p.toString()
}
