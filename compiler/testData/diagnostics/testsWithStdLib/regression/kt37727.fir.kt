// RUN_PIPELINE_TILL: FRONTEND
data class A(val x: Set<<!UNRESOLVED_REFERENCE!>CLassNotFound<!>> = <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>setOf<!>()) {
    fun with(x: Set<<!UNRESOLVED_REFERENCE!>CLassNotFound<!>>? = null) {
        A(x ?: this.x)
    }
}
