// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-63377
// FIR_DUMP

class OuterClass<OuterParam> {
    class OuterParam

    fun <NestedParam : OuterParam> foo(t: NestedParam) {
        val k: OuterParam = t
        val l: OuterParam <!INITIALIZER_TYPE_MISMATCH!>=<!> OuterParam()
    }

    inner class Inner<NestedParam : OuterParam>(t: NestedParam) {
        val k: OuterParam = t
        val l: OuterParam <!INITIALIZER_TYPE_MISMATCH!>=<!> OuterParam()

        init {
            val m: OuterParam = t
            val n: OuterParam <!INITIALIZER_TYPE_MISMATCH!>=<!> OuterParam()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, init, inner, localProperty, nestedClass, nullableType,
primaryConstructor, propertyDeclaration, typeConstraint, typeParameter */
