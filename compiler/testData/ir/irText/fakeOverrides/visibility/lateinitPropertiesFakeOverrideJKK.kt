// FIR_IDENTICAL
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: JKKClassForFakeOverride.java
public class JKKClassForFakeOverride extends KKClassForFakeOverride {
    public void test() {
        String valuePub = this.publicProp;
        String valueProt = this.protectedProp;
    }

    public void test2(KKClassForFakeOverride instance) {
        String valuePub = instance.publicProp;
        String valueProt = instance.protectedProp;
    }
}

// FILE: KClass.kt
open class KClass {
    open lateinit var publicProp: String
    protected lateinit var protectedProp: String
    private lateinit var privateProp: String
}

open class KKClassForFakeOverride : KClass() {
}
