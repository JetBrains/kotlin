inline class A(val x: Int)
class B(val a: A = A(0))

// @B.class:
// 1 private <init>\(I\)V
// 1 public synthetic <init>\(IILkotlin/jvm/internal/DefaultConstructorMarker;\)V
// 1 public synthetic <init>\(ILkotlin/jvm/internal/DefaultConstructorMarker;\)V

// JVM_TEMPLATES
// 1 private <init>\(\)V

// JVM_IR_TEMPLATES
// 1 public <init>\(\)V
