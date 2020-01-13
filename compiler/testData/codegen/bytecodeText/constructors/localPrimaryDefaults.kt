class A(val s: String) {
    fun f(): Int {
        class B(val x: Int = 0) {
            fun f(): Int = x
        }
        return B().f()
    }
}

// @A.class:
// 1 public <init>\(Ljava/lang/String;\)V

// @A$f$B.class:
// 0 <init>\(\)V
// 1 public <init>\(I\)V
// 1 public synthetic <init>\(IILkotlin/jvm/internal/DefaultConstructorMarker;\)V
