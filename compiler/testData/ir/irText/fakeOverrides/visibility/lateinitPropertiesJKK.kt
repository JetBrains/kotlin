// FIR_IDENTICAL
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: JKKClass.java
public class JKKClass extends KKClass {
    public void test() {
        var valueInitPub = this.initializedPublicProp;
        var valueInitProt = this.initializedProtectedProp;

        var valuePub = this.publicProp;
        var valueProt = this.protectedProp;

        var valuePriv = this.privateProp;

    }

    public void test2(KKClass instance) {
        var valueInitPriv = instance.initializedPublicProp;
        var valueInitProt = instance.initializedProtectedProp;

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

    open var initializedPublicProp = "test"
    protected open var initializedProtectedProp = "test"

}

open class KKClass :  KClass() {
    override lateinit var initializedPublicProp: String
    override lateinit var initializedProtectedProp: String
}
