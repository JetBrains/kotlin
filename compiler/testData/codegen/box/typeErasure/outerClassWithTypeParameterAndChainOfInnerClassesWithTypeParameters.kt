// The test should be moved to a new test directive when KT-80645 is done
// WITH_STDLIB
class A<T : CharSequence>(val a: T) {
    inner class Inner1<I1 : CharSequence>(val b: T, val c: I1) {
        inner class Inner2<I2 : CharSequence>(val d: T, val e: I1, val f: I2) {
                inline fun foo(g: T, h: I1, i: I2) = a.length +
                        b.length + c.length +
                        d.length + e.length + f.length +
                        g.length + h.length + i.length

                fun bar(g: T, h: I1, i: I2) = a.length +
                        b.length + c.length +
                        d.length + e.length + f.length +
                        g.length + h.length + i.length
        }
    }
}

fun box(): String {
    val arguments = listOf<String>("0123456789", "", "\n")
    for (arg in arguments)
        if (A(arg).Inner1("0", "01").Inner2("0", "01", "012").foo("0", "01", "012") !=
            A(arg).Inner1("0", "01").Inner2("0", "01", "012").bar("0", "01", "012"))
            return arg
    return "OK"
}

//FUN name:foo signature:/A.Inner1.Inner2.foo|foo(3:0;2:0;1:0){}[0] visibility:public modality:FINAL returnType:kotlin.Int [inline]
//  VALUE_PARAMETER kind:Regular name:<this> index:0 type:<root>.A.Inner1.Inner2<kotlin.CharSequence, kotlin.CharSequence, kotlin.CharSequence>
//  VALUE_PARAMETER kind:Regular name:g index:1 type:kotlin.CharSequence
//  VALUE_PARAMETER kind:Regular name:h index:2 type:kotlin.CharSequence
//  VALUE_PARAMETER kind:Regular name:i index:3 type:kotlin.CharSequence
//  BLOCK_BODY
//      RETURN type=kotlin.Nothing from='/A.Inner1.Inner2.foo|foo(3:0;2:0;1:0){}[0]'
//          CALL 'kotlin/Int.plus|plus(kotlin.Int){}[0]' type=kotlin.Int origin=PLUS
//              ARG 1: CALL 'kotlin/Int.plus|plus(kotlin.Int){}[0]' type=kotlin.Int origin=PLUS
//                  ARG 1: CALL 'kotlin/Int.plus|plus(kotlin.Int){}[0]' type=kotlin.Int origin=PLUS
//                      ARG 1: CALL 'kotlin/Int.plus|plus(kotlin.Int){}[0]' type=kotlin.Int origin=PLUS
//                          ARG 1: CALL 'kotlin/Int.plus|plus(kotlin.Int){}[0]' type=kotlin.Int origin=PLUS
//                              ARG 1: CALL 'kotlin/Int.plus|plus(kotlin.Int){}[0]' type=kotlin.Int origin=PLUS
//                                  ARG 1: CALL 'kotlin/Int.plus|plus(kotlin.Int){}[0]' type=kotlin.Int origin=PLUS
//                                      ARG 1: CALL 'kotlin/Int.plus|plus(kotlin.Int){}[0]' type=kotlin.Int origin=PLUS
//                                          ARG 1: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                                              ARG 1: CALL '/A.a.<get-a>|<get-a>(){}[0]' type=kotlin.CharSequence origin=GET_PROPERTY
//                                                  ARG 1: CALL '/A.Inner1.Inner2.access$<outer-this-1>|access$<outer-this-1>#static(A.Inner1.Inner2<1:0,2:0,3:0>){}[0]' type=<root>.A<kotlin.CharSequence> origin=null
//                                                      ARG 1: GET_VAR '<this>: <root>.A.Inner1.Inner2<kotlin.CharSequence, kotlin.CharSequence, kotlin.CharSequence> declared in <root>.foo' type=<root>.A.Inner1.Inner2<kotlin.CharSequence, kotlin.CharSequence, kotlin.CharSequence> origin=null
//                                          ARG 2: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                                              ARG 1: CALL '/A.Inner1.b.<get-b>|<get-b>(){}[0]' type=kotlin.CharSequence origin=GET_PROPERTY
//                                                  ARG 1: CALL '/A.Inner1.Inner2.access$<outer-this-0>|access$<outer-this-0>#static(A.Inner1.Inner2<1:0,2:0,3:0>){}[0]' type=<root>.A.Inner1<kotlin.CharSequence, kotlin.CharSequence> origin=null
//                                                      ARG 1: GET_VAR '<this>: <root>.A.Inner1.Inner2<kotlin.CharSequence, kotlin.CharSequence, kotlin.CharSequence> declared in <root>.foo' type=<root>.A.Inner1.Inner2<kotlin.CharSequence, kotlin.CharSequence, kotlin.CharSequence> origin=null
//                                      ARG 2: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                                          ARG 1: CALL '/A.Inner1.c.<get-c>|<get-c>(){}[0]' type=kotlin.CharSequence origin=GET_PROPERTY
//                                              ARG 1: CALL '/A.Inner1.Inner2.access$<outer-this-0>|access$<outer-this-0>#static(A.Inner1.Inner2<1:0,2:0,3:0>){}[0]' type=<root>.A.Inner1<kotlin.CharSequence, kotlin.CharSequence> origin=null
//                                                  ARG 1: GET_VAR '<this>: <root>.A.Inner1.Inner2<kotlin.CharSequence, kotlin.CharSequence, kotlin.CharSequence> declared in <root>.foo' type=<root>.A.Inner1.Inner2<kotlin.CharSequence, kotlin.CharSequence, kotlin.CharSequence> origin=null
//                                  ARG 2: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                                      ARG 1: CALL '/A.Inner1.Inner2.d.<get-d>|<get-d>(){}[0]' type=kotlin.CharSequence origin=GET_PROPERTY
//                                          ARG 1: GET_VAR '<this>: <root>.A.Inner1.Inner2<kotlin.CharSequence, kotlin.CharSequence, kotlin.CharSequence> declared in <root>.foo' type=<root>.A.Inner1.Inner2<kotlin.CharSequence, kotlin.CharSequence, kotlin.CharSequence> origin=IMPLICIT_ARGUMENT
//                              ARG 2: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                                  ARG 1: CALL '/A.Inner1.Inner2.e.<get-e>|<get-e>(){}[0]' type=kotlin.CharSequence origin=GET_PROPERTY
//                                      ARG 1: GET_VAR '<this>: <root>.A.Inner1.Inner2<kotlin.CharSequence, kotlin.CharSequence, kotlin.CharSequence> declared in <root>.foo' type=<root>.A.Inner1.Inner2<kotlin.CharSequence, kotlin.CharSequence, kotlin.CharSequence> origin=IMPLICIT_ARGUMENT
//                          ARG 2: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                              ARG 1: CALL '/A.Inner1.Inner2.f.<get-f>|<get-f>(){}[0]' type=kotlin.CharSequence origin=GET_PROPERTY
//                                  ARG 1: GET_VAR '<this>: <root>.A.Inner1.Inner2<kotlin.CharSequence, kotlin.CharSequence, kotlin.CharSequence> declared in <root>.foo' type=<root>.A.Inner1.Inner2<kotlin.CharSequence, kotlin.CharSequence, kotlin.CharSequence> origin=IMPLICIT_ARGUMENT
//                      ARG 2: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                          ARG 1: GET_VAR 'g: kotlin.CharSequence declared in <root>.foo' type=kotlin.CharSequence origin=null
//                  ARG 2: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                      ARG 1: GET_VAR 'h: kotlin.CharSequence declared in <root>.foo' type=kotlin.CharSequence origin=null
//              ARG 2: CALL 'kotlin/CharSequence.length.<get-length>|<get-length>(){}[0]' type=kotlin.Int origin=GET_PROPERTY
//                  ARG 1: GET_VAR 'i: kotlin.CharSequence declared in <root>.foo' type=kotlin.CharSequence origin=null