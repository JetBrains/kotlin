enum class E(private val value: Int) {
    A(0) {
        override fun f() {}
    },
    B(1) {
        override fun f() {}
    };

    abstract fun f()
}

// The JVM BE creates an accessor for the constructor of `E`, but not for the constructors of the enum entry classes E$A and E$B.
// 1 public synthetic <init>\(Ljava/lang/String;IILkotlin/jvm/internal/DefaultConstructorMarker;\)V
// 2 INVOKESPECIAL E.<init> \(Ljava/lang/String;IILkotlin/jvm/internal/DefaultConstructorMarker;\)V
// 3 kotlin/jvm/internal/DefaultConstructorMarker;\)