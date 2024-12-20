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
        var valuePub = this.publicProp;
        var valueProt = this.protectedProp;

        var valuePriv = this.privateProp;
    }

    public void test(KClass instance) {
        var valuePub = instance.publicProp;
        var valueProt = instance.protectedProp;

        var valuePriv = instance.privateProp;
    }
}

