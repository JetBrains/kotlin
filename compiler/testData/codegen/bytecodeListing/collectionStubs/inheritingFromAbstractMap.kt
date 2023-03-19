// WITH_STDLIB

// IGNORE_BACKEND_K2: JVM_IR
// FIR status:
//   1) KT-57268 K2: extra methods `remove` and/or `getOrDefault` are generated for Map subclasses with JDK 1.6 in dependencies
//   2) KT-57300 K2: subclass of MutableCollection with primitive element type has methods with boxed type
//      (`containsValue(Ljava/lang/Double;)Z` instead of `containsValue(D)Z`)

abstract class AMapSD : AbstractMap<String, Double>()

abstract class AMMapSD : AbstractMutableMap<String, Double>()
