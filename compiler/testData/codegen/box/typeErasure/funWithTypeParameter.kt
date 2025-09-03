// The test should be moved to a new test directive when KT-80645 is done
// WITH_STDLIB

inline fun <T> foo(a: T) = a.toString()

fun <T> bar(a: T) = a.toString()

fun box(): String {
    val arguments = listOf<Any?>("0123456789", 4.20, true, null)
    for (arg in arguments)
        if (foo(arg) != bar(arg))
            return arg.toString()
    return "OK"
}

//FUN name:foo signature:/foo|foo(0:0){0§<kotlin.Any?>}[0] visibility:public modality:FINAL returnType:kotlin.String [inline]
//  TYPE_PARAMETER name:T index:0 variance: signature:[ /foo|foo(kotlin.Any?){0§<kotlin.Any?>}[0] <- Local[<TP>,0] ] superTypes:[kotlin.Any?] reified:false
//  VALUE_PARAMETER kind:Regular name:a index:0 type:kotlin.Any?
//  BLOCK_BODY
//      RETURN type=kotlin.Nothing from='/foo|foo(0:0){0§<kotlin.Any?>}[0]'
//          CALL 'kotlin/toString|toString@kotlin.Any?(){}[0]' type=kotlin.String origin=null
//              ARG 1: GET_VAR 'a: kotlin.Any? declared in <root>.foo' type=kotlin.Any? origin=null