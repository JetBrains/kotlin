// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76806

class WithInner<T> {
    fun compareObjs() = Obj == Obj

    <!WRONG_MODIFIER_TARGET!>inner<!> object Obj
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, localClass, nestedClass, nullableType,
objectDeclaration, typeParameter */
