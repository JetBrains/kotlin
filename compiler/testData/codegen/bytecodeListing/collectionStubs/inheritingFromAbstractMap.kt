// WITH_STDLIB

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57300 K2: subclass of MutableCollection with primitive element type has methods with boxed type
//      (`containsValue(Ljava/lang/Double;)Z` instead of `containsValue(D)Z`)

abstract class AMapSD : AbstractMap<String, Double>()

abstract class AMMapSD : AbstractMutableMap<String, Double>()
