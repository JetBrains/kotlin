// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS

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
    override val p1: <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE("val p1: Int; 'val p1: String' defined in 'A'")!>Int<!>
    @An
    override <!VAR_OVERRIDDEN_BY_VAL("'var p2: String' defined in 'A'; val p2: String")!>val<!> p2: @An String
    override fun test(arg: String): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE("fun test(arg: String): Int; 'fun test(arg: String): String' defined in 'A'")!>Int<!>
}

interface C : A {
    override var p2: <!VAR_TYPE_MISMATCH_ON_OVERRIDE("var p2: Int; 'var p2: String' defined in 'A'")!>Int<!>
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, interfaceDeclaration, override, propertyDeclaration */
