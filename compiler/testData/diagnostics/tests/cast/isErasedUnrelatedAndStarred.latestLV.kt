// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
class G<T>
interface Tr

fun f(q: Tr) = <!IMPOSSIBLE_IS_CHECK_ERROR!>q is G<*><!>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, isExpression, nullableType,
starProjection, typeParameter */
