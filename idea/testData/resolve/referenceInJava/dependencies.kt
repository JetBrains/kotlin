package k

import kotlin.platform.platformStatic

public class Class() {
    public val prop: Int = 0
    fun function() = 1
}

public enum class EnumClass {
    ENTRY
}


public fun topLevelFun() {
}

public class ClassWithClassObject {
    companion object {
        fun f() = 1
    }
}

public object KotlinObject {
    fun f() = 1
}

public interface StaticFieldInClassObjectInTrait {
    companion object {
        public val XX: String = "xx"
    }
}

object PlatformStaticFun {
    platformStatic
    fun test() {
    }
}

interface TraitNoImpl {
    fun foo()
}

public class TraitWithDelegatedNoImpl(f: TraitNoImpl): TraitNoImpl by f

interface TraitWithImpl {
    fun foo() = 1
}

public class TraitWithDelegatedWithImpl(f: TraitWithImpl) : TraitWithImpl by f

kotlin.jvm.jvmOverloads
public fun withJvmOverloads(i: Int, b: Boolean = false, s: String="hello") {}


