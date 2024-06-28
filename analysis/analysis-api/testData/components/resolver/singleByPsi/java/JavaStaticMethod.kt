// FILE: pkg/Foo.java

package pkg;

public class Foo {
    public static void bar() {
        System.out.println("Foo.bar()");
    }
}

// FILE: main.kt
import pkg.Foo

fun box() {
    // NB: if we put bar<caret>(), LPAR, value arguments, function call, containing function and file will be visited in order.
    //   Among them, only function call can be converted to [KtReference], and the corresponding form is [KtInvokeFunctionReference],
    //   whose resolution picks implicit invoke() only, hence failed. That is expected for reference resolution with both FE1.0 and FIR.
    //   In reality, i.e., in IDE, users will click the part of function call "name", which results in simple name reference instead.
    Foo.ba<caret>r()
}
