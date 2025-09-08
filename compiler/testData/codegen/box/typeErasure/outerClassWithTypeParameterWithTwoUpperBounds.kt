// The test should be moved to a new test directive when KT-80645 is done
// WITH_STDLIB
class A<T>(val a: T) where T : CharSequence, T : Comparable<T> {
    inline fun foo(b: T) = a.length + b.length

    fun bar(b: T) = a.length + b.length
}

fun box(): String {
    val arguments = listOf<String>("0123456789", "", "\n")
    for (arg in arguments)
        if (A(arg).foo(arg) != A(arg).bar(arg))
            return arg
    return "OK"
}

//FUN name:foo signature:/A.foo|foo(1:0){}[0] visibility:public modality:FINAL returnType:kotlin.Int [inline]
//  VALUE_PARAMETER kind:Regular name:<this> index:0 type:<root>.A<kotlin.CharSequence>
//  VALUE_PARAMETER kind:Regular name:b index:1 type:kotlin.CharSequence
//  BLOCK_BODY
//      RETURN type=kotlin.Nothing from='/A.foo|foo(1:0){}[0]'
//          CALL 'kotlin/Int.plus|plus(kotlin.Int){}[0]' type=kotlin.Int origin=PLUS
//              ARG 1: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                  ARG 1: CALL '/A.a.<get-a>|<get-a>(){}[0]' type=kotlin.CharSequence origin=GET_PROPERTY
//                      ARG 1: GET_VAR '<this>: <root>.A<kotlin.CharSequence> declared in <root>.foo' type=<root>.A<kotlin.CharSequence> origin=IMPLICIT_ARGUMENT
//              ARG 2: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                  ARG 1: GET_VAR 'b: kotlin.CharSequence declared in <root>.foo' type=kotlin.CharSequence origin=null