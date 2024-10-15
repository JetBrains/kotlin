// RUN_PIPELINE_TILL: FRONTEND
val a = l@ {
    return@l <!UNRESOLVED_REFERENCE!>r<!>
}

val b = l@ {
    if ("" == "OK") return@l

    return@l <!RETURN_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>r<!>
}