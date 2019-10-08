// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm

class Outer {
    class Inner {
        fun f() { assert(true) }
    }
}

// We set the assertion status based on top-level classes.
// 0 LDC LOuter\$Inner;.class
// 1 desiredAssertionStatus
// The assertion disabled field should be package local.
// 1 final static synthetic Z \$assertionsDisabled
// 0 public final static synthetic Z \$assertionsDisabled
// 0 protected final static synthetic Z \$assertionsDisabled
// 0 private final static synthetic Z \$assertionsDisabled
// Outer\$Inner.<clinit>:
// 1 LDC LOuter;.class\s*INVOKEVIRTUAL java/lang/Class.desiredAssertionStatus \(\)Z
// 1 PUTSTATIC Outer\$Inner.\$assertionsDisabled : Z
// Outer\$Inner.f:
// 1 GETSTATIC Outer\$Inner.\$assertionsDisabled