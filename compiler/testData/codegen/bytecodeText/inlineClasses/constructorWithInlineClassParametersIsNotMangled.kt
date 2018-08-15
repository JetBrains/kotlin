// !LANGUAGE: +InlineClasses

inline class Str(val s: String)

class C1(val ss: Str)

class C2(val ss1: Str, val ss2: Str)

// 2 public \<init\>\(Ljava/lang/String;\)V
// 1 public \<init\>\(Ljava/lang/String;Ljava/lang/String;\)V