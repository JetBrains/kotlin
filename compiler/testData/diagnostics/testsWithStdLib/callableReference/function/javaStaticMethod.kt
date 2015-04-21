// !CHECK_TYPE
// FILE: test/A.java

package test

import java.util.Arrays;

public class A {
    public static void main(String[] args) {
        System.out.println(Arrays.asList(args));
    }
}

// FILE: 1.kt

import kotlin.reflect.*
import test.A

fun foo(args: Array<String>) {
    val main2 = A::main
    checkSubtype<KFunction1<Array<String>, Unit>>(main2)
    main2(args)
    (A::main)(args)
}
