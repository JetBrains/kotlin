// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Str(val string: String)

class C {
    var s = Str("")
}

// 1 public final getS-fpuCDAk\(\)Ljava/lang/String;
// 1 public final setS-pD0jJn0\(Ljava/lang/String;\)V