// DUMP_IR
// IGNORE_BACKEND_K1: JVM_IR
// ^After KT-78111, K2 generates indy bytecode in this test. K1 doesn't which fails the CHECK_BYTECODE_TEXT.
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
