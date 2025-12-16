// FIR_IDENTICAL
// DONT_TARGET_EXACT_BACKEND: JS_IR
// ISSUE: KT-82590

// Caused by: kotlin.reflect.jvm.internal.KotlinReflectionInternalError: Unknown origin of public abstract operator fun invoke(p1: P1): R defined in // kotlin.Function1[FunctionInvokeDescriptor@6e593174] (class kotlin.reflect.jvm.internal.impl.builtins.functions.FunctionInvokeDescriptor)
//     at kotlin.reflect.jvm.internal.RuntimeTypeMapper.mapSignature(RuntimeTypeMapper.kt:208)
//     at kotlin.reflect.jvm.internal.DescriptorKFunction.<init>(DescriptorKFunction.kt:52)
//     at kotlin.reflect.jvm.internal.CreateKCallableVisitor.visitFunctionDescriptor(util.kt:400)
//     at kotlin.reflect.jvm.internal.CreateKCallableVisitor.visitFunctionDescriptor(util.kt:372)
//     at kotlin.reflect.jvm.internal.impl.descriptors.impl.FunctionDescriptorImpl.accept(FunctionDescriptorImpl.java:826)
//     at kotlin.reflect.jvm.internal.KClassImpl.getMembers(KClassImpl.kt:453)
//     at kotlin.reflect.jvm.internal.KClassImpl.access$getMembers(KClassImpl.kt:66)
//     at kotlin.reflect.jvm.internal.KClassImpl$Data.declaredNonStaticMembers_delegate$lambda$0(KClassImpl.kt:381)
// SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK

interface MyInterface

fun interface SimpleRibCoroutineWorker<E> {
    suspend fun onStart(scope: E)
}

class FooWorker<F> : SimpleRibCoroutineWorker<F>, (String) -> Unit {
    override suspend fun onStart(scope: F) {
    }

    override fun invoke(str: String) { /* no op*/ }
}

fun <T1> foo(x: SimpleRibCoroutineWorker<T1>) {}
fun <T2> bar(vararg x: SimpleRibCoroutineWorker<T2>) {}

fun createNewPlugin(): SimpleRibCoroutineWorker<MyInterface> {
    val x: SimpleRibCoroutineWorker<MyInterface> = FooWorker()

    return FooWorker()
}

fun main() {
    foo<MyInterface>(FooWorker())
    bar<MyInterface>(FooWorker())
}