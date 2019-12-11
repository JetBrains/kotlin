// FILE: KSuper.kt

interface KSuper {
    fun foo(ksuperName: Int)
}

// FILE: JSuper1.java

interface JSuper1 {
    void foo(int jsuper1Name);
}

// FILE: JSuper2.java

interface JSuper2 {
    void foo(int jsuper2Name);
}


// FILE: Sub1.java
interface Sub1 extends KSuper, JSuper1, JSuper2 {
    @Override
    void foo(int sub1Name);
}

// FILE: Sub2.java
interface Sub2 extends JSuper1, KSuper, JSuper2 {
    @Override
    void foo(int sub2Name);
}

// FILE: Sub3.java
interface Sub3 extends JSuper1, JSuper2, KSuper {
    @Override
    void foo(int sub3Name);
}


// FILE: SubSub.kt

class SubSub1 : Sub1 {
    override fun foo(ksuperName: Int) {}
}

class SubSub2 : Sub2 {
    override fun foo(ksuperName: Int) {}
}

class SubSub3 : Sub3 {
    override fun foo(ksuperName: Int) {}
}
