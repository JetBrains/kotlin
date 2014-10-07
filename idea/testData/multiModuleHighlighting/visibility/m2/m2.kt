package m2

import m1.*

fun testVisibility() {
    PublicClassInM1()

    InternalClassInM1()

    <error descr="[INVISIBLE_MEMBER] Cannot access 'PrivateClassInM1': it is 'private' in 'm1'">PrivateClassInM1</error>()

    publicFunInM1()

    internalFunInM1()

    <error descr="[INVISIBLE_MEMBER] Cannot access 'privateFunInM1': it is 'private' in 'm1'">privateFunInM1</error>()
}

public class ClassInM2

public class B: A() {

    fun accessA(<warning>a</warning>: A) {}

    fun f() {
        <error descr="[INVISIBLE_MEMBER] Cannot access 'pri': it is 'invisible_fake' in 'B'">pri</error>()

        pro()

        pub()

        int()
    }
}