// The test should be moved to a new test directive when KT-80645 is done
// WITH_STDLIB
// LANGUAGE: +ContextParameters
context(c: Int)
inline fun <T : CharSequence> foo(a: T) = a.length + c

context(c: Int)
fun <T : CharSequence> bar(a: T) = a.length + c

fun box(): String = with(42) {
    val arguments = listOf<String>("0123456789", "", "\n")
    for (arg in arguments)
        if (foo(arg) != bar(arg))
            return arg
    return "OK"
}

//FUN name:foo signature:/foo|foo(kotlin.Int)(0:0){0§<kotlin.CharSequence>}[0] visibility:public modality:FINAL returnType:kotlin.Int [inline]
//  TYPE_PARAMETER name:T index:0 variance: signature:[ /foo|foo(kotlin.Int;kotlin.CharSequence){0§<kotlin.CharSequence>}[0] <- Local[<TP>,0] ] superTypes:[kotlin.CharSequence] reified:false
//  VALUE_PARAMETER kind:Regular name:c index:0 type:kotlin.Int
//  VALUE_PARAMETER kind:Regular name:a index:1 type:kotlin.CharSequence
//  BLOCK_BODY
//      RETURN type=kotlin.Nothing from='/foo|foo(kotlin.Int)(0:0){0§<kotlin.CharSequence>}[0]'
//          CALL 'kotlin/Int.plus|plus(kotlin.Int){}[0]' type=kotlin.Int origin=PLUS
//              ARG 1: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                  ARG 1: GET_VAR 'a: kotlin.CharSequence declared in <root>.foo' type=kotlin.CharSequence origin=null
//              ARG 2: GET_VAR 'c: kotlin.Int declared in <root>.foo' type=kotlin.Int origin=null