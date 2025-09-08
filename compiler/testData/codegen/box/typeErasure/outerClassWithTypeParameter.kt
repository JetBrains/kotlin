// The test should be moved to a new test directive when KT-80645 is done
// WITH_STDLIB
class A<T>(val a: T) {
    inline fun foo(b: T) = a.toString() + b.toString()

    fun bar(b: T) = a.toString() + b.toString()
}

fun box(): String {
    val arguments = listOf<Any?>("0123456789", 4.20, true, null, A(null), A(A("")))
    for (arg in arguments)
        if (A(arg).foo(arg) != A(arg).bar(arg))
            return arg.toString()
    return "OK"
}

//FUN name:foo signature:/A.foo|foo(1:0){}[0] visibility:public modality:FINAL returnType:kotlin.String [inline]
//  VALUE_PARAMETER kind:Regular name:<this> index:0 type:<root>.A<kotlin.Any?>
//  VALUE_PARAMETER kind:Regular name:b index:1 type:kotlin.Any?
//  BLOCK_BODY
//      RETURN type=kotlin.Nothing from='/A.foo|foo(1:0){}[0]'
//          CALL 'kotlin/String.plus|plus(kotlin.Any?){}[0]' type=kotlin.String origin=PLUS
//              ARG 1: CALL 'kotlin/toString|toString@kotlin.Any?(){}[0]' type=kotlin.String origin=null
//                  ARG 1: CALL '/A.a.<get-a>|<get-a>(){}[0]' type=kotlin.Any? origin=GET_PROPERTY
//                      ARG 1: GET_VAR '<this>: <root>.A<kotlin.Any?> declared in <root>.foo' type=<root>.A<kotlin.Any?> origin=IMPLICIT_ARGUMENT
//              ARG 2: CALL 'kotlin/toString|toString@kotlin.Any?(){}[0]' type=kotlin.String origin=null
//                  ARG 1: GET_VAR 'b: kotlin.Any? declared in <root>.foo' type=kotlin.Any? origin=null