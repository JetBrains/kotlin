// ISSUE: KT-41721

// FILE: SAM.java
public interface SAM<T> {
  void apply(T x);
}

// FILE: A.java
public class A<T> {
  public void call(SAM<T>... block) { block[0].apply(null); }
}

// FILE: main.kt
fun test(x: A<*>) {
    x.call({ y: Any? -> Unit })
}
