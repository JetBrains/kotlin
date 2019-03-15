// !JVM_DEFAULT_MODE: enable
// !JVM_TARGET: 1.8

// FILE: JavaInterface.java
public interface JavaInterface {
    default void test() {}

    default void testForNonDefault() {}

    void testAbstract();
}

// FILE: 1.kt

interface KotlinInterface : JavaInterface {
    @JvmDefault
    override fun test() {}

    <!NON_JVM_DEFAULT_OVERRIDES_JAVA_DEFAULT!>override fun testForNonDefault()<!> {}

    override fun testAbstract() {}
}

interface KotlinInterface2 : JavaInterface, KotlinInterface {
    @JvmDefault
    override fun test() {}

    override fun testForNonDefault() {}

    override fun testAbstract() {}
}


interface KotlinInterfaceForIndirect : JavaInterface {

}

interface KotlinInterfaceIndirectInheritance : KotlinInterfaceForIndirect {

    @JvmDefault
    override fun test() {}

    <!NON_JVM_DEFAULT_OVERRIDES_JAVA_DEFAULT!>override fun testForNonDefault()<!> {}

    override fun testAbstract() {}
}

open class KotlinClass : JavaInterface {
    override fun test() {}

    override fun testForNonDefault() {}

    override fun testAbstract() {}
}

interface KotlinInterfaceX  {

    fun test() {}

    fun testForNonDefault() {}

    fun testAbstract() {}
}

interface KotlinInterfaceManySuper: JavaInterface, KotlinInterfaceX {
    @JvmDefault
    override fun test() {}

    <!NON_JVM_DEFAULT_OVERRIDES_JAVA_DEFAULT!>override fun testForNonDefault()<!> {}

    override fun testAbstract() {}
}
