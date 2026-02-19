// LANGUAGE: +InlineClasses

inline class A(val s: String)

class B(x: Long, a: A = A("OK"))

// @B.class:
// 1 private <init>\(JLjava/lang/String;\)V
// 1 public synthetic <init>\(JLjava/lang/String;Lkotlin/jvm/internal/DefaultConstructorMarker;\)V
// 1 public synthetic <init>\(JLjava/lang/String;ILkotlin/jvm/internal/DefaultConstructorMarker;\)V
// 0 <init>\(JLjava/lang/String;ILkotlin/jvm/internal/DefaultConstructorMarker;Lkotlin/jvm/internal/DefaultConstructorMarker;\)V
