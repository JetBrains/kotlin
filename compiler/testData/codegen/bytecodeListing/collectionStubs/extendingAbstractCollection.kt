// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57299 K2: VerifyError due to overriding final method `size` on a subclass of Collection and Set

abstract class AC<X> : Collection<X>

abstract class ASet<T> : AC<T>(), Set<T>
