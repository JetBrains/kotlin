// TARGET_BACKEND: JVM

// MODULE: old
// FILE: test2/Row.java

package test2;

public interface Row {
    String res();
}

// MODULE: new(old)

// FILE: test1/Row.java
package test1;

public interface Row {
    String res();
}

// FILE: JavaClass.java

public class JavaClass {
    public static test1.Row foo() {
        return new test1.Row() {
            @Override
            public String res() {
                return "OK";
            }
        };
    }

    public static String bar(test1.Row y) { return y.res(); }
    public static String bar(test2.Row y) { return y.res(); }
}

// MODULE: main(new)
// FILE: main.kt
fun box(): String {
    val r = JavaClass.foo()
    return JavaClass.bar(r)
}
