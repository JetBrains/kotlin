// !JVM_TARGET: 1.6
// FILE: JavaInterfaceBase.java

public interface JavaInterfaceBase {
    default String test() {
        return "OK";
    }

    default String testOverride() {
        return "OK";
    }
}

// FILE: JavaInterface.java

public interface JavaInterface extends JavaInterfaceBase {
    static String testStatic() {
        return "OK";
    }
}


// FILE: 1.kt
import JavaInterface.testStatic

interface KotlinInterface : JavaInterface {
    fun fooo() {
        testStatic()
        super.test()
        test()
        testOverride()
    }

    override fun testOverride(): String {
        return "OK";
    }
}

interface KotlinInterfaceIndirectInheritance : KotlinInterface {
    fun foooo() {
        testStatic()
        super.test()
        testOverride()
        super.testOverride()
    }
}

open class KotlinClass : JavaInterface {
    fun foo(){
        testStatic()
        super.test()
        super.testOverride()
    }
}

class KotlinClassIndirectInheritance : KotlinClass() {
    fun foo2(){
        testStatic()
        super.test()
        super.testOverride()
    }
}

class KotlinClassIndirectInheritance2 : KotlinInterfaceIndirectInheritance {
    fun foo(){
        testStatic()
        super.test()
        super.testOverride()
    }
}

fun test() {
    JavaInterface.testStatic()
    KotlinClass().foo()
    KotlinClassIndirectInheritance2().foo()

    KotlinClass().test()
    KotlinClass().testOverride()
    KotlinClassIndirectInheritance().testOverride()
}
