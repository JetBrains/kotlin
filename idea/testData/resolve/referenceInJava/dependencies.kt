package k

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