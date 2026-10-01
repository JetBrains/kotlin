// LL_FIR_DIVERGENCE
// LL tests don't have jvmTargetProvider, so no class is known to be a value object at run time there.
// See isValueObjectAtRuntime
// ISSUE: KT-81100
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// TARGET_BACKEND: JVM
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// VALHALLA_VALUE_CLASSES
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21
import java.lang.ref.Cleaner
import java.lang.ref.ReferenceQueue
import java.util.IdentityHashMap
import java.util.WeakHashMap
import java.util.function.ToIntFunction
import java.util.stream.Stream

value class Full(val x: Int, val y: Int)

fun interface KotlinSam<T> {
    fun apply(t: T): Int
}

fun interface SwappedSam<R, T> {
    fun apply(t: T): R
}

fun interface ReceiverSam<T> {
    fun T.apply(): Int
}

fun <T> javaSam(t: T, f: ToIntFunction<T>) = f.applyAsInt(t)
fun <T> kotlinSam(t: T, f: KotlinSam<T>) = f.apply(t)
fun <T> swappedSam(t: T, f: SwappedSam<Int, T>) = f.apply(t)
fun <T> receiverSam(t: T, f: ReceiverSam<T>) = with(f) { t.apply() }

fun samConversions(full: Full) {
    javaSam(full, System::identityHashCode)
    kotlinSam(full, System::identityHashCode)
    swappedSam(full, System::identityHashCode)
    receiverSam(full, System::identityHashCode)
    Stream.of(full).mapToInt(System::identityHashCode)
}

open class Holder<T>(t: T, f: (T) -> Int)

class SuperCall(full: Full) : Holder<Full>(full, System::identityHashCode)

fun nonArguments(cleaner: Cleaner): (Full) -> Int {
    val property: (Full) -> Int = System::identityHashCode
    val register: (Full, Runnable) -> Cleaner.Cleanable = cleaner::register
    val unbound: (Cleaner, Full, Runnable) -> Cleaner.Cleanable = Cleaner::register
    var assigned: (Full) -> Int = { 0 }
    assigned = System::identityHashCode
    val vararg = listOf<(Full) -> Int>(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>System::identityHashCode<!>)
    return System::identityHashCode
}

fun otherPositions(flag: Boolean, full: Full, nullable: ((Full) -> Int)?, default: (Full) -> Int = System::identityHashCode) {
    val branch: (Full) -> Int = if (flag) System::identityHashCode else { _ -> 0 }
    val elvis: (Full) -> Int = nullable ?: System::identityHashCode
    javaSam(f = System::identityHashCode, t = full)
}

fun constructorReferences(): () -> IdentityHashMap<Full, Int> {
    val weak: () -> WeakHashMap<Full, Int> = ::WeakHashMap
    val queue: () -> ReferenceQueue<Full> = ::ReferenceQueue
    lazy<IdentityHashMap<Full, Int>>(::IdentityHashMap)
    return ::IdentityHashMap
}

fun identityClass(cleaner: Cleaner): (String) -> Int {
    val register: (String, Runnable) -> Cleaner.Cleanable = cleaner::register
    val weak: () -> WeakHashMap<String, Int> = ::WeakHashMap
    javaSam("", System::identityHashCode)
    return System::identityHashCode
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, elvisExpression, flexibleType, funInterface,
funWithExtensionReceiver, functionDeclaration, functionalType, ifExpression, inProjection, integerLiteral,
interfaceDeclaration, javaCallableReference, javaFunction, lambdaLiteral, localProperty, nullableType,
primaryConstructor, propertyDeclaration, samConversion, stringLiteral, typeParameter, value */
