// LANGUAGE: -JvmEnhancedBridges

interface I

abstract class T3<K, V : I> : MutableMap<K, V> {
}

// @T3.class:
// 1 public synthetic bridge remove\(Ljava/lang/Object;\)Ljava/lang/Object;
// 1 INVOKEVIRTUAL T3.remove \(Ljava/lang/Object;\)Ljava/lang/Object;
// 1 public synthetic bridge get\(Ljava/lang/Object;\)Ljava/lang/Object;
// 1 INVOKEVIRTUAL T3.get \(Ljava/lang/Object;\)Ljava/lang/Object;