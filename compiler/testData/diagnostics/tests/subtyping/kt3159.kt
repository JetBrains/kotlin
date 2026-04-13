// RUN_PIPELINE_TILL: FRONTEND
interface Super {
    var v: CharSequence
    val v2: CharSequence
}

class Sub: Super {
    override var v: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>String<!> = "fail"
    override val v2: String = "ok"
}

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, override, propertyDeclaration, stringLiteral */
