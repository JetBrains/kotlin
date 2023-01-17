// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_BACKEND: NATIVE
// FILE: Processor.java

public interface Processor<T> {
    boolean process(T t);
}

// FILE: test.kt

interface PsiModifierListOwner
interface KtClassOrObject {
    fun toLightClass(): PsiModifierListOwner?
}

fun execute(declaration: Any, consumer: Processor<in PsiModifierListOwner>) {
    when (declaration) {
        is KtClassOrObject -> {
            val lightClass = declaration.toLightClass()
            consumer.process(lightClass)
        }
    }
}

fun box(): String = "OK"
