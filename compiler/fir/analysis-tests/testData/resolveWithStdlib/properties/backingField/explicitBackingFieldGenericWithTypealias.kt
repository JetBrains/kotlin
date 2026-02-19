// RUN_PIPELINE_TILL: FRONTEND

class WithTypealias<T> {
    val a: Any
        field = 1

    fun usage(x: Alias<T>): Int = x.a
    fun <T> usage2(x: Alias<T>): Int = x.a
    fun usage3(x: Alias2): Int = x.a
}

fun <T> usage(x: Alias<T>): Int = <!RETURN_TYPE_MISMATCH!>x.a<!>
fun usage2(x: Alias2): Int = <!RETURN_TYPE_MISMATCH!>x.a<!>

typealias Alias<T> = WithTypealias<T>
typealias Alias2 = WithTypealias<Int>

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, integerLiteral, nullableType,
propertyDeclaration, smartcast, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
