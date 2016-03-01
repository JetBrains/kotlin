// FILE: test/Foo.java

package test;

public class Foo {
    public enum MyEnum {
        A;
    }
}

// FILE: 1.kt

import test.*
import test.Foo.MyEnum.A

fun box() =
    if (Foo.MyEnum.A.toString() == "A" && A.toString() == "A") "OK"
    else "fail"
