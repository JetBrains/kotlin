// FIR_IDENTICAL
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: JKKClass.java
public class JKKClass extends KKClass {
    public void test() {
        String valueInitPub = this.initializedPublicProp;
        String valueInitProt = this.initializedProtectedProp;

        String valuePub = this.publicProp;
        String valueProt = this.protectedProp;

    }

    public void test2(KKClass instance) {
        String valueInitPriv = instance.initializedPublicProp;
        String valueInitProt = instance.initializedProtectedProp;

        String valuePub = instance.publicProp;
        String valueProt = instance.protectedProp;
    }
}

// FILE: KClass.kt
open class KClass {
    open lateinit var publicProp: String
    protected lateinit var protectedProp: String
    private lateinit var privateProp: String

    open var initializedPublicProp = "test"
    protected open var initializedProtectedProp = "test"

}

open class KKClass :  KClass() {
    override lateinit var initializedPublicProp: String
    override lateinit var initializedProtectedProp: String
}
