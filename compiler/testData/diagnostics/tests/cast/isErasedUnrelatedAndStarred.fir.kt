// RUN_PIPELINE_TILL: BACKEND
class G<T>
interface Tr

fun f(q: Tr) = <!USELESS_IS_CHECK!>q is G<*><!>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, isExpression, nullableType,
starProjection, typeParameter */
