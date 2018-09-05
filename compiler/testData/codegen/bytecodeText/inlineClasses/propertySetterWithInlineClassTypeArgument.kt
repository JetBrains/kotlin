// !LANGUAGE: +InlineClasses

inline class Str(val string: String)

class C {
    var s = Str("")
}

// 1 public final getS\(\)Ljava/lang/String;
// 1 public final setS-90215lrx\(Ljava/lang/String;\)V