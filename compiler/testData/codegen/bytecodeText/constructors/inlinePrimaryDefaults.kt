// LANGUAGE: +InlineClasses
// This tests both KT-37013 and KT-37015.

inline class A(val x: Int = 0)

// 0 <init>\(\)V
// 1 private synthetic <init>\(I\)V
// 1 public final static synthetic box-impl\(I\)LA;
// 1 public static constructor-impl\(I\)I
// 1 public static synthetic constructor-impl\$default\(IILkotlin/jvm/internal/DefaultConstructorMarker;\)I
