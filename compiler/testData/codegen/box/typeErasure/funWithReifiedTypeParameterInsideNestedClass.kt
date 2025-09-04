// The test should be moved to a new test directive when KT-80645 is done
// WITH_STDLIB
// WITH_REFLECTION
import kotlin.reflect.typeOf

class A {
    class Nested {
        inline fun <reified T : CharSequence> foo(a: T) = typeOf<T>()
    }
}

fun box(): String {
    val arguments = listOf<String>("0123456789", "", "\n")
    for (arg in arguments)
        if (A.Nested().foo(arg) != typeOf<String>())
            return arg
    return "OK"
}

//FUN name:foo signature:/A.Nested.foo|foo(0:0){0§<kotlin.CharSequence>}[0] visibility:public modality:FINAL returnType:kotlin.reflect.KType [inline]
//  TYPE_PARAMETER name:T index:0 variance: signature:[ /foo|foo(A.Nested;0:0){0§<kotlin.CharSequence>}[0] <- Local[<TP>,0] ] superTypes:[kotlin.CharSequence] reified:true
//  VALUE_PARAMETER kind:Regular name:<this> index:0 type:<root>.A.Nested
//  VALUE_PARAMETER kind:Regular name:a index:1 type:T of <root>.foo
//  BLOCK_BODY
//      RETURN type=kotlin.Nothing from='/A.Nested.foo|foo(0:0){0§<kotlin.CharSequence>}[0]'
//          CALL 'kotlin.reflect/typeOf|typeOf(){0§<kotlin.Any?>}[0]' type=kotlin.reflect.KType origin=null
//              TYPE_ARG 1: T of <root>.foo