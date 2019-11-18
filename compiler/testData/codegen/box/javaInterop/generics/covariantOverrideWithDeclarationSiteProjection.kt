// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME
// FILE: JavaClass.java

public class JavaClass {

    public static class C extends B {
        public OutPair<String, Integer> foo() {
            return super.foo();
        }

        public In<Object> bar() {
            return super.bar();
        }
    }

    public static String test() {
        A a = new C();

        if (!a.foo().getX().equals("OK")) return "fail 1";
        if (!a.foo().getY().equals(123)) return "fail 2";

        if (!a.bar().make("123").equals("123")) return "fail 3";

        return "OK";
    }
}

// FILE: main.kt

class OutPair<out X, out Y>(val x: X, val y: Y)
class In<in Z> {
    fun make(x: Z): String = x.toString()
}

interface A {
    fun foo(): OutPair<@JvmWildcard CharSequence, @JvmSuppressWildcards(false) Number>
    fun bar(): In<@JvmWildcard String>
}

abstract class B : A {
    override fun foo(): OutPair<String, Int> = OutPair("OK", 123)
    override fun bar(): In<Any> = In()
}

fun box(): String {
    return JavaClass.test();
}
