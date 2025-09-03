// The test should be moved to a new test directive when KT-80645 is done
// WITH_STDLIB
inline fun <T : CharSequence> Int.foo(a: T) = a.length + this

fun <T : CharSequence> Int.bar(a: T) = a.length + this

fun box(): String {
    val arguments = listOf<String>("0123456789", "", "\n")
    for (arg in arguments)
        if (42.foo(arg) != 42.bar(arg))
            return arg
    return "OK"
}

//FUN name:foo signature:/foo|foo@kotlin.Int(0:0){0§<kotlin.CharSequence>}[0] visibility:public modality:FINAL returnType:kotlin.Int [inline]
//  TYPE_PARAMETER name:T index:0 variance: signature:[ /foo|foo(kotlin.Int;kotlin.CharSequence){0§<kotlin.CharSequence>}[0] <- Local[<TP>,0] ] superTypes:[kotlin.CharSequence] reified:false
//  VALUE_PARAMETER kind:Regular name:<this> index:0 type:kotlin.Int
//  VALUE_PARAMETER kind:Regular name:a index:1 type:kotlin.CharSequence
//  BLOCK_BODY
//      RETURN type=kotlin.Nothing from='/foo|foo@kotlin.Int(0:0){0§<kotlin.CharSequence>}[0]'
//          CALL 'kotlin/Int.plus|plus(kotlin.Int){}[0]' type=kotlin.Int origin=PLUS
//              ARG 1: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                  ARG 1: GET_VAR 'a: kotlin.CharSequence declared in <root>.foo' type=kotlin.CharSequence origin=null
//              ARG 2: GET_VAR '<this>: kotlin.Int declared in <root>.foo' type=kotlin.Int origin=null