// The test should be moved to a new test directive when KT-80645 is done
// WITH_STDLIB
inline fun <T> foo(a: T)
        where T : CharSequence,
              T : Comparable<T> =
    a.length

fun <T> bar(a: T)
        where T : CharSequence,
              T : Comparable<T> =
    a.length

fun box(): String {
    val arguments = listOf<String>("0123456789", "", "\n")
    for (arg in arguments)
        if (foo(arg) != bar(arg))
            return arg
    return "OK"
}

//FUN name:foo signature:/foo|foo(0:0){0§<kotlin.CharSequence&kotlin.Comparable<0:0>>}[0] visibility:public modality:FINAL returnType:kotlin.Int [inline]
//  TYPE_PARAMETER name:T index:0 variance: signature:[ /foo|foo(kotlin.CharSequence){0§<kotlin.CharSequence&kotlin.Comparable<kotlin.CharSequence>>}[0] <- Local[<TP>,0] ] superTypes:[kotlin.CharSequence; kotlin.Comparable<kotlin.CharSequence>] reified:false
//  VALUE_PARAMETER kind:Regular name:a index:0 type:kotlin.CharSequence
//  BLOCK_BODY
//      RETURN type=kotlin.Nothing from='/foo|foo(0:0){0§<kotlin.CharSequence&kotlin.Comparable<0:0>>}[0]'
//          CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//              ARG 1: GET_VAR 'a: kotlin.CharSequence declared in <root>.foo' type=kotlin.CharSequence origin=null