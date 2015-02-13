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
    class object {
        fun f() = 1
    }
}

public object KotlinObject {
    fun f() = 1
}

public trait StaticFieldInClassObjectInTrait {
    class object {
        public val XX: String = "xx"
    }
}

object PlatformStaticFun {
    platformStatic
    fun test() {
    }
}

trait TraitNoImpl {
    fun foo()
}