// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-83770

interface IAny { val y: Any }
interface ICharSeq { val y: CharSequence }

open class H<T:Any>(x: T) {
    val y: Any
        field: T = x

    fun testMulti(b: H<String>) {
        if (b is IAny && b is ICharSeq) {
            val s: String <!INITIALIZER_TYPE_MISMATCH!>=<!> b.y
            b.y.length
        }
    }
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, explicitBackingField, functionDeclaration, ifExpression,
isExpression, localProperty, primaryConstructor, propertyDeclaration, smartcast, typeConstraint, typeParameter */
