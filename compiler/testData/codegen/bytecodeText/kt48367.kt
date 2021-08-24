fun foo(block: () -> String): String = block()
inline fun bar(crossinline f: () -> String) = foo { f() }

fun flaf() {
  val revoked = "A"
  bar {
    synchronized (revoked) {
      "B"
    }
  }
}

// The field $revoked$inlined should be loaded only once and stored in a local
// that is used for the monitor enter/exit instructions. Locking and unlocking
// directly on a field load makes it hard for the JVM to prove that locking is
// balanced which causes the code to be interpreted. See KT-48367 for details.

// 1 GETFIELD Kt48367Kt\$flaf\$\$inlined\$bar\$1.\$revoked\$inlined : Ljava/lang/String;
// 1 MONITORENTER
// 2 MONITOREXIT
