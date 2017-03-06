// FILE: JavaInterface.java

public interface JavaInterface {
    static String testStatic() {
        return "OK";
    }

    default String test() {
        return "OK";
    }

    default String testOverride() {
        return "OK";
    }
}

// FILE: 1.kt
import JavaInterface.testStatic

interface KotlinInterface : JavaInterface {
    fun fooo() {
        testStatic()
        super.test()
    }

    override fun testOverride(): String {
        return "OK";
    }
}

interface KotlinInterfaceInderectInheritance : KotlinInterface {
    fun foooo() {
        testStatic()
        super.test()
    }
}

open class KotlinClass : JavaInterface {
    fun foo(){
        testStatic()
        super.test()
        super.testOverride()
    }
}

class KotlinClassInderectInheritance : KotlinClass() {
    fun foo2(){
        testStatic()
        super.test()
        super.testOverride()
    }
}

class KotlinClassInderectInheritance2 : KotlinInterfaceInderectInheritance {
    fun foo(){
        testStatic()
        super.test()
        super.testOverride()
    }
}

fun test() {
    JavaInterface.testStatic()
    KotlinClass().foo()
    KotlinClassInderectInheritance2().foo()

    KotlinClass().test()
    KotlinClass().testOverride()
    KotlinClassInderectInheritance().testOverride()
}
