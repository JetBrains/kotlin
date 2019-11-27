/*
 * There is some complex rules for conversions from java method `get...` to property
 *   (see `JavaSyntheticPropertiesScope`), but they are not supported in FIR
 * It's possible to support them in `JavaClassUseSiteMemberScope`
 * But problem is that we also have `FirSyntheticPropertiesScope` that creates
 *   synthetic properties for everithig
 *
 * Because of that such code is also resolves incorrect:
 *
 * class A {
 *     fun getX(): Int = 1
 * }
 *
 * fun test(a: A) {
 *     a.x // resolves to `getX`
 * }
 */

// FILE: A.java

public class A {
    public String getVMParameters() {
        return null;
    }
}

// FILE: B.java

public class B {
    public Integer getVmParameters() {
        return null;
    }
}

// FILE: C.java

public class C {
    public String getVMParameters() {
        return null;
    }

    public Integer getVmParameters() {
        return null;
    }
}

// FILE: main.kt

fun test_1(x: A) {
    val str1 = x.<!UNRESOLVED_REFERENCE!>vmParameters<!> // OK
    val str2 = x.vMParameters // should be error
}

fun test_2(x: B) {
    val int = x.vmParameters // OK
    val error = x.<!UNRESOLVED_REFERENCE!>vMParameters<!> // should be error
}

fun test_3(x: C) {
    val error = x.vmParameters // should be error
    val int = x.vMParameters // should be error
}

class Foo {
    fun getX(): Int = 1
}

fun test_4(foo: Foo) {
    foo.x // should be error
}