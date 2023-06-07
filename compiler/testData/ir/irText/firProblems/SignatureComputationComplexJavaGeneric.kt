// FIR_IDENTICAL
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-57022

// FILE: JavaClass1.java

import org.jetbrains.annotations.Nullable;

public class JavaClass1<T extends @Nullable Object> {
    public class A {
        public void output(T x) {}
    }
    public class B extends A {
    }
}

// FILE: JavaClass2.java
import org.jetbrains.annotations.Nullable;

public abstract class JavaClass2<T extends @Nullable Object, R extends @Nullable Object> {

  public abstract class A {
    public abstract void output(R output);
  }

  public abstract class B extends A {
    public abstract T element();
  }
}

// FILE: test.kt

class Inv<T>(val x: T)

class Test_1<TT>(val x: TT) : JavaClass1<TT>() {
    fun test(b: B) { b.output(x) }
}

class Test_2<TT> : JavaClass2<TT, Inv<TT>>() {
    fun process(b: B) {
        b.output(Inv(b.element()))
    }
}

fun <R> test_3(jb: JavaClass1<R>.B, r: R) {
    jb.output(r)
}
