// TARGET_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-62865

// TODO support different bytecode text templates for FIR?
// --CHECK_BYTECODE_TEXT
// --JVM_IR_TEMPLATES
// --2 java/lang/invoke/LambdaMetafactory

// FILE: test.kt

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

// FILE: Consumer.java

public interface Consumer<T> {
    void accept(T t);
}
