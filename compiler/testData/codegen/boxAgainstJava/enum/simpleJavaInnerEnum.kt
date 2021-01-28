// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: test/Foo.java

package test;

public class Foo {
    public enum MyEnum {
        A;
    }
}

// MODULE: main(lib)
// FILE: 1.kt

import test.*
import test.Foo.MyEnum.A

fun box() =
    if (Foo.MyEnum.A.toString() == "A" && A.toString() == "A") "OK"
    else "fail"
