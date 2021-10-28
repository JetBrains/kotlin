// WITH_RUNTIME
class A<T>(val x: String, val y: String, val z: T)

suspend fun <T> foo(a: A<T>, block: suspend (A<T>) -> String): String = block(a)

operator fun A<*>.component1() = x

object B {
    operator fun A<*>.component2() = y
}

suspend fun B.bar(): String {
    operator fun <R> A<R>.component3() = z

    return foo(A("O", "K", 123)) { (x_param, y_param, z_param) ->
        x_param + y_param + z_param.toString()
    }
}

suspend fun test() = B.bar()

// Local function bodies (i.e., `A<R>.component3()`) are in a separate class (implementing FunctionN) for non-IR, and are static methods
// in the enclosing class for IR. Therefore the ordinal in the suspend lambda class name is different for non-IR (`$3`) vs IR (e.g., `$2`).
//
// JVM_TEMPLATES
// METHOD : ExtensionComponentsKt$bar$3.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
// VARIABLE : NAME=$dstr$x_param$y_param$z_param TYPE=LA; INDEX=2
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=3
// VARIABLE : NAME=y_param TYPE=Ljava/lang/String; INDEX=4
// VARIABLE : NAME=z_param TYPE=I INDEX=5
// VARIABLE : NAME=this TYPE=LExtensionComponentsKt$bar$3; INDEX=0
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=1

// JVM_IR_TEMPLATES
// METHOD : ExtensionComponentsKt$bar$2.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=2
// VARIABLE : NAME=y_param TYPE=Ljava/lang/String; INDEX=3
// VARIABLE : NAME=z_param TYPE=I INDEX=4
// VARIABLE : NAME=this TYPE=LExtensionComponentsKt$bar$2; INDEX=0
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=1
