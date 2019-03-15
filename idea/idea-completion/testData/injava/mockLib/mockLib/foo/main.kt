package mockLib.foo

public class LibClass {
    public fun foo() {
    }

    companion object {
        fun classObjectFun() {
        }

        public object NestedObject
    }

    public class Nested {
        public val valInNested: Int = 1
        public fun funInNested() {
        }
    }

    public val nested: Nested = Nested()
}

public interface LibTrait {
    public fun foo() {
    }
}

public enum class LibEnum {
    RED,
    GREEN,
    BLUE
}

public object LibObject

public fun topLevelFunction(): String = ""

public fun String.topLevelExtFunction(): String = ""

public var topLevelVar: String = ""

class F() {
    companion object {
        class F {
            companion object {
                object F {
                }
            }
        }
    }
}

interface MyInterface {
    fun foo() = 1
}

annotation class Anno(val c: Int = 3, val d: String)