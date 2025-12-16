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

fun interface SimpleRibCoroutineWorker {
    suspend fun onStart(scope: MyInterface)
}

class FooWorker : SimpleRibCoroutineWorker, (String) -> Unit {
    override suspend fun onStart(scope: MyInterface) {
    }

    override fun invoke(str: String) { /* no op*/ }
}

fun foo(x: SimpleRibCoroutineWorker) {}
fun bar(vararg x: SimpleRibCoroutineWorker) {}

fun createNewPlugin(): SimpleRibCoroutineWorker {
    val x: SimpleRibCoroutineWorker = FooWorker()

    return FooWorker()
}

fun main() {
    foo(FooWorker())
    bar(FooWorker())
}