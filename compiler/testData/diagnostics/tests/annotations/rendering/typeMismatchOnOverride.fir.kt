// RENDER_DIAGNOSTICS_MESSAGES

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS,  AnnotationTarget.PROPERTY,  AnnotationTarget.VALUE_PARAMETER)
annotation class An

@An
interface A {
    @An
    val p1: @An String
    @An
    var p2: @An String
    @An
    fun test(@An arg: @An String): @An String
}

@An
interface B : A {
    override val p1: <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE("p1; '@An() val p1: @An() String' defined in '/A'")!>Int<!>
    @An
    override <!VAR_OVERRIDDEN_BY_VAL("'@An() var p2: @An() String' defined in '/A'; @An() val p2: @An() String")!>val<!> p2: @An String
    override fun test(arg: String): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE("test; '@An() fun test(@An() arg: @An() String): @An() String' defined in '/A'")!>Int<!>
}

interface C : A {
    override var p2: <!VAR_TYPE_MISMATCH_ON_OVERRIDE("p2; '@An() var p2: @An() String' defined in '/A'")!>Int<!>
}
