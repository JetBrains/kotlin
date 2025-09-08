// The test should be moved to a new test directive when KT-80645 is done
// WITH_STDLIB
class A<T : CharSequence>(val a: T) {
    inner class Inner<I : CharSequence>(val b: T, val c: I) {
        inline fun foo(d: T, e: I) = a.length +
                b.length + c.length +
                d.length + e.length

        fun bar(d: T, e: I) = a.length +
                b.length + c.length +
                d.length + e.length
    }
}

fun box(): String {
    val arguments = listOf<String>("0123456789", "", "\n")
    for (arg in arguments)
        if (A(arg).Inner("0", "01").foo("012", "0123") != A(arg).Inner("0", "01").bar("012", "0123"))
            return arg
    return "OK"
}

//FUN name:foo signature:/A.Inner.foo|foo(2:0;1:0){}[0] visibility:public modality:FINAL returnType:kotlin.Int [inline]
//  VALUE_PARAMETER kind:Regular name:<this> index:0 type:<root>.A.Inner<kotlin.CharSequence, kotlin.CharSequence>
//  VALUE_PARAMETER kind:Regular name:d index:1 type:kotlin.CharSequence
//  VALUE_PARAMETER kind:Regular name:e index:2 type:kotlin.CharSequence
//  BLOCK_BODY
//      RETURN type=kotlin.Nothing from='/A.Inner.foo|foo(2:0;1:0){}[0]'
//          CALL 'kotlin/Int.plus|plus(kotlin.Int){}[0]' type=kotlin.Int origin=PLUS
//              ARG 1: CALL 'kotlin/Int.plus|plus(kotlin.Int){}[0]' type=kotlin.Int origin=PLUS
//                  ARG 1: CALL 'kotlin/Int.plus|plus(kotlin.Int){}[0]' type=kotlin.Int origin=PLUS
//                      ARG 1: CALL 'kotlin/Int.plus|plus(kotlin.Int){}[0]' type=kotlin.Int origin=PLUS
//                          ARG 1: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                              ARG 1: CALL '/A.a.<get-a>|<get-a>(){}[0]' type=kotlin.CharSequence origin=GET_PROPERTY
//                                  ARG 1: CALL '/A.Inner.access$<outer-this-0>|access$<outer-this-0>#static(A.Inner<1:0,2:0>){}[0]' type=<root>.A<kotlin.CharSequence> origin=null
//                                      ARG 1: GET_VAR '<this>: <root>.A.Inner<kotlin.CharSequence, kotlin.CharSequence> declared in <root>.foo' type=<root>.A.Inner<kotlin.CharSequence, kotlin.CharSequence> origin=null
//                          ARG 2: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                              ARG 1: CALL '/A.Inner.b.<get-b>|<get-b>(){}[0]' type=kotlin.CharSequence origin=GET_PROPERTY
//                                  ARG 1: GET_VAR '<this>: <root>.A.Inner<kotlin.CharSequence, kotlin.CharSequence> declared in <root>.foo' type=<root>.A.Inner<kotlin.CharSequence, kotlin.CharSequence> origin=IMPLICIT_ARGUMENT
//                      ARG 2: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                          ARG 1: CALL '/A.Inner.c.<get-c>|<get-c>(){}[0]' type=kotlin.CharSequence origin=GET_PROPERTY
//                              ARG 1: GET_VAR '<this>: <root>.A.Inner<kotlin.CharSequence, kotlin.CharSequence> declared in <root>.foo' type=<root>.A.Inner<kotlin.CharSequence, kotlin.CharSequence> origin=IMPLICIT_ARGUMENT
//                  ARG 2: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                      ARG 1: GET_VAR 'd: kotlin.CharSequence declared in <root>.foo' type=kotlin.CharSequence origin=null
//              ARG 2: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                  ARG 1: GET_VAR 'e: kotlin.CharSequence declared in <root>.foo' type=kotlin.CharSequence origin=nul