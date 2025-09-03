// The test should be moved to a new test directive when KT-80645 is done
// WITH_STDLIB
inline fun <T : Number> foo(a: T) = a.toString()

fun <T : Number> bar(a: T) = a.toString()

fun box(): String {
    val arguments = listOf<Number>(42, 4.20, -0, 1L, 0x0F, 0b00001011, 123.5e10, 123.5f, 1_000_000)
    for (arg in arguments)
        if (foo(arg) != bar(arg))
            return arg.toString()
    return "OK"
}

//FUN name:foo signature:/foo|foo(0:0){0§<kotlin.Number>}[0] visibility:public modality:FINAL returnType:kotlin.String [inline]
//  TYPE_PARAMETER name:T index:0 variance: signature:[ /foo|foo(kotlin.Number){0§<kotlin.Number>}[0] <- Local[<TP>,0] ] superTypes:[kotlin.Number] reified:false
//  VALUE_PARAMETER kind:Regular name:a index:0 type:kotlin.Number
//  BLOCK_BODY
//      RETURN type=kotlin.Nothing from='/foo|foo(0:0){0§<kotlin.Number>}[0]'
//          CALL 'kotlin/Any.toString|toString(){}[0]' type=kotlin.String origin=null
//              ARG 1: GET_VAR 'a: kotlin.Number declared in <root>.foo' type=kotlin.Number origin=null