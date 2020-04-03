enum class Enum(val x: Int = 0) {
    A,
    B(0) { override fun f() {} };
    open fun f() {}
}

// @Enum.class:
// 0 <init>\(\)V
// 1 private <init>\(Ljava/lang/String;II\)V
// 1 synthetic <init>\(Ljava/lang/String;IIILkotlin/jvm/internal/DefaultConstructorMarker;\)V
// 1 public synthetic <init>\(Ljava/lang/String;IILkotlin/jvm/internal/DefaultConstructorMarker;\)V

// @Enum$B.class:
// 0 <init>\(\)V
// 1 <init>\(Ljava/lang/String;I\)V