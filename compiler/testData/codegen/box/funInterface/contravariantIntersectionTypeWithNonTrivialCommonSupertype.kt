// CHECK_BYTECODE_TEXT
// 0 java/lang/invoke/LambdaMetafactory

interface Top

interface Common : Top

abstract class BaseClass : Common
interface BaseInterface : Common

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

class GenericHolder<T : Top> {
    fun doOnSuccess(onSuccess: Consumer<in T>) {
        onSuccess.accept(object : BaseClass() {} as T)
    }
}

fun interface Consumer<T : Top> {
    fun accept(t: T)
}
