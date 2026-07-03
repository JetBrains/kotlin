// DUMP_IR
// DUMP_IR_DIFFERENCE: JVM
//   K/JVM: extra IMPLICIT_CAST inside SAM_CONVERSION
//   SAM_CONVERSION type is on K/JVM: `Consumer<kotlin.Any?>`, on non-JVM: `Consumer<out kotlin.Any>`
// CHECK_BYTECODE_TEXT
// 1 java/lang/invoke/LambdaMetafactory

abstract class BaseClass
interface BaseInterface

class ConcreteType : BaseClass(), BaseInterface
class ConcreteType2 : BaseClass(), BaseInterface

fun box(): String {
    example(0)
    return "OK"
}

fun example(input: Int) {
    val instance = when (input) {
        0 -> GenericHolder<ConcreteType>()
        else -> GenericHolder<ConcreteType2>()
    }

    instance.doOnSuccess {}
    instance.doOnSuccess(::functionReference)
}

fun functionReference(x: Any) {}

class GenericHolder<T> {
    fun doOnSuccess(onSuccess: Consumer<in T>) {
        onSuccess.accept(object : BaseClass() {} as T)
    }
}

fun interface Consumer<T> {
    fun accept(t: T)
}
