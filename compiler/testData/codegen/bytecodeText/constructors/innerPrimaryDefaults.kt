class A(val s: String) {
    inner class B(val x: Int = 0)
}

// @A.class
// 1 public <init>\(Ljava/lang/String;\)V

// @A$B.class
// 0 <init>\(\)V
// 0 <init>\(LA;\)V
// 1 public <init>\(LA;I\)V
// 1 public synthetic <init>\(LA;IILkotlin/jvm/internal/DefaultConstructorMarker;\)V
