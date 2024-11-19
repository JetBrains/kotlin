// RUN_PIPELINE_TILL: FRONTEND
val a = l@ <!UNRESOLVED_REFERENCE!>{
    return@l <!UNRESOLVED_REFERENCE!>r<!>
}<!>

val b = l@ {
    if ("" == "OK") return@l

    return@l <!UNRESOLVED_REFERENCE!>r<!>
}