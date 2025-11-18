// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76683

interface ColumnsSelectionDsl<E> {
    fun getMyValue(): E?
}
interface DataFrame<F>

interface Column<F>

typealias ColumnsSelector<T, C> = ColumnsSelectionDsl<T>.() -> Column<C>

// for getting the median of any specific number type (so `Double`, `Int`, etc. but not `Number`)
fun <T, C> DataFrame<T>.median(columns: ColumnsSelector<T, C>): Double where C : Number?, C : Comparable<C & Any>? = TODO()

// for getting the median of any other comparable type
fun <T, C : Comparable<C & Any>?> DataFrame<T>.median(columns: ColumnsSelector<T, C>): C & Any = TODO()

fun <X> X.toColumn(): Column<X> = TODO()

fun foo(df: DataFrame<String>) {
    val x1 = df.median { getMyValue()?.length.toColumn() }
    val x2 = df.median { getMyValue()?.length?.toDouble().toColumn() }
    val x3 = df.median {
        val t = getMyValue()
        "$t.".toColumn()
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Double")!>x1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Double")!>x2<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x3<!>
}

/* GENERATED_FIR_TAGS: dnnType, funWithExtensionReceiver, functionDeclaration, functionalType, interfaceDeclaration,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, safeCall, stringLiteral, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter, typeWithExtension */
