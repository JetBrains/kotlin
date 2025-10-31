// RUN_PIPELINE_TILL: FRONTEND

class AList<T>() : List<T> by <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>inner<!> {
    private val inner = ArrayList<T>()
}

open class X(bar: Int)

class Y : X(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>bar<!>) {
    val bar = 4
}
class Y2 : X(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.bar) {
    val bar = 4
}
class Y3 : X(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this@Y3<!>.bar) {
    val bar = 4
}

/* GENERATED_FIR_TAGS: classDeclaration, inheritanceDelegation, integerLiteral, nullableType, primaryConstructor,
propertyDeclaration, typeParameter */
