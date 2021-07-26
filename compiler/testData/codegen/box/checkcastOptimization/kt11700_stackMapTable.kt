// TARGET_BACKEND: JVM_IR
// WITH_RUNTIME
// FILE: Outer.java
public class Outer {
    public class Enclosed {
        public SubtypeOfOuter getSub1() {
            return new SubtypeOfOuter();
        }
        public SubtypeOfOuter getSub2() {
            return new SubtypeOfOuter();
        }
    }

    public class SubtypeOfOuter extends Outer {
    }
}

// FILE: test.kt
fun Outer.Enclosed.calc(num: Int): Outer? {
    return if (num == 1) {
        this.sub1
    } else if (num == 2) {
        Outer.Enclosed::class.java.getDeclaredMethod("getSub1").invoke(this) as Outer?
    } else {
        this.sub2
    }
}

fun box(): String {
    Outer().Enclosed().calc(1)
    Outer().Enclosed().calc(2)
    Outer().Enclosed().calc(3)
    return "OK"
}