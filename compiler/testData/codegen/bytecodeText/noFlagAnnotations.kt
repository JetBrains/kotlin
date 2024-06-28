@Volatile var vol = 1
@kotlin.concurrent.Volatile var vol2 = 1
@Transient val tra = 1

@Strictfp fun str() {}
@Synchronized fun sync() {}

// 0 kotlin/jvm/Volatile
// 0 kotlin/concurrent/Volatile
// 0 kotlin/jvm/Transient
// 0 kotlin/jvm/Strictfp
// 0 kotlin/jvm/Synchronized
