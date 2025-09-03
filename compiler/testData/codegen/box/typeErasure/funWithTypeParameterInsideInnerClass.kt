// The test should be moved to a new test directive when KT-80645 is done
// WITH_STDLIB
class A {
    inner class Inner {
        inline fun <T : CharSequence> foo(a: T) = a.length

        fun <T : CharSequence> bar(a: T) = a.length
    }
}

fun box(): String {
    val arguments = listOf<String>("0123456789", "", "\n")
    for (arg in arguments)
        if (A().Inner().foo(arg) != A().Inner().bar(arg))
            return arg
    return "OK"
}

//FUN name:foo signature:/A.Inner.foo|foo(0:0){0§<kotlin.CharSequence>}[0] visibility:public modality:FINAL returnType:kotlin.Int [inline]
//  TYPE_PARAMETER name:T index:0 variance: signature:[ /foo|foo(A.Inner;kotlin.CharSequence){0§<kotlin.CharSequence>}[0] <- Local[<TP>,0] ] superTypes:[kotlin.CharSequence] reified:false
//  VALUE_PARAMETER kind:Regular name:<this> index:0 type:<root>.A.Inner
//  VALUE_PARAMETER kind:Regular name:a index:1 type:kotlin.CharSequence
//  BLOCK_BODY
//      RETURN type=kotlin.Nothing from='/A.Inner.foo|foo(0:0){0§<kotlin.CharSequence>}[0]'
//          CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//              ARG 1: GET_VAR 'a: kotlin.CharSequence declared in <root>.foo' type=kotlin.CharSequence origin=null