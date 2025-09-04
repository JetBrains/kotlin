// The test should be moved to a new test directive when KT-80645 is done
// WITH_STDLIB
// WITH_REFLECTION
import kotlin.reflect.typeOf

inline fun <reified T : Number> foo(a: T) = typeOf<T>()

fun box(): String {
    val arguments = listOf<Number>(42, 4.20, -0, 1L, 0x0F, 0b00001011, 123.5e10, 123.5f, 1_000_000)
    for (arg in arguments)
        if (foo(arg) != typeOf<Number>())
            return arg.toString()
    return "OK"
}

//FUN name:foo signature:/foo|foo(0:0){0§<kotlin.Number>}[0] visibility:public modality:FINAL returnType:kotlin.reflect.KType [inline]
//  TYPE_PARAMETER name:T index:0 variance: signature:[ /foo|foo(0:0){0§<kotlin.Number>}[0] <- Local[<TP>,0] ] superTypes:[kotlin.Number] reified:true
//  VALUE_PARAMETER kind:Regular name:a index:0 type:T of <root>.foo
//  BLOCK_BODY
//      RETURN type=kotlin.Nothing from='/foo|foo(0:0){0§<kotlin.Number>}[0]'
//          CALL 'kotlin.reflect/typeOf|typeOf(){0§<kotlin.Any?>}[0]' type=kotlin.reflect.KType origin=null
//              TYPE_ARG 1: T of <root>.foo