// FIR_IDENTICAL
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// MODULE: separate

// FILE: KClass.kt

open class KClass {
    open lateinit var publicProp: String
    protected lateinit var protectedProp: String
    private lateinit var privateProp: String
}

// MODULE: main(separate)

// FILE: JKClass.java
public class JKClass extends KClass {
    public void test() {
        String valuePub = this.publicProp;
        String valueProt = this.protectedProp;
    }

    public void test(KClass instance) {
        String valuePub = instance.publicProp;
        String valueProt = instance.protectedProp;
    }
}

