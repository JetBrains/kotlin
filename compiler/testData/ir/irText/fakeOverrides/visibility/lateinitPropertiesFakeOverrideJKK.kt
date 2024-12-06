// FIR_IDENTICAL
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: JKKClassForFakeOverride.java
public class JKKClassForFakeOverride extends KKClassForFakeOverride {
    public void test() {
        var valuePub = this.publicProp;
        var valueProt = this.protectedProp;

        var valuePriv = this.privateProp;
    }

    public void test2(KKClassForFakeOverride instance) {
        var valuePub = instance.publicProp;
        var valueProt = instance.protectedProp;

        var valuePriv = instance.privateProp;
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