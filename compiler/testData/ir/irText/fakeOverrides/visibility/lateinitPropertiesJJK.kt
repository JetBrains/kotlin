// FIR_IDENTICAL
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: JKClass.java
public class JKClass extends KClass {
    public void test() {
        String valuePub = this.publicProp;
        String valueProt = this.protectedProp;
    }

    public void test(KClass instance) {
        String valuePub = instance.publicProp;
        String valueProt = instance.protectedProp; // no error in the same package
    }
}

// FILE: JJKClass.java
public class JJKClass extends JKClass {
    public void test() {
        String valuePub = this.publicProp;
        String valueProt = this.protectedProp; // no error in the same package
    }

    public void test(JKClass instance) {
        String valuePub = instance.publicProp;
        String valueProt = instance.protectedProp; // no error in the same package
    }
}

// FILE: KClass.kt
open class KClass {
    open lateinit var publicProp: String
    protected lateinit var protectedProp: String
    private lateinit var privateProp: String
}
