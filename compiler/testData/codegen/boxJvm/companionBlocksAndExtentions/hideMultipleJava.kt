// TARGET_BACKEND: JVM
// LANGUAGE: +CompanionBlocks

// FILE: A.java
public class A {
    public static String sFoo(int x) {
        return "A::sFoo(int)";
    }

    public static String sFoo(Integer x) {
        return "A::sFoo(Integer)";
    }

    public String mFoo(int x) {
        return "A::mFoo(int)";
    }

    public String mFoo(Integer x) {
        return "A::mFoo(Integer)";
    }
}

// FILE: B.kt
open class B : A() {
    companion {
        fun sFoo(x: Int?) = "B::sFoo(Int?)"
    }

    override fun mFoo(x: Int?) = "B::mFoo(Int?)"
}

// FILE: J.java
class J extends B {
    String box() {
        return mFoo(1) + " " + mFoo(Integer.valueOf(1)) + " " +
            sFoo(1) + " " + sFoo(Integer.valueOf(1));
    }
}

// FILE: test.kt

class K : B() {
    fun box() = mFoo(1) + " " + mFoo(1 as Int?) + " " +
        sFoo(1) + " " + sFoo(1 as Int?)
}

fun box(): String {
    if (J().box() != "A::mFoo(int) B::mFoo(Int?) A::sFoo(int) B::sFoo(Int?)") return "fail J"
    if (K().box() != "A::mFoo(int) B::mFoo(Int?) B::sFoo(Int?) B::sFoo(Int?)") return "fail K"
    return "OK"
}
