// FILE: ConstVal.java
package test;

@Ann(ABC.TOP_LEVEL + A.OBJECT + B.COMPANION)
class Java {

}

// FILE: ConstVal.kt
@file:JvmName("ABC")
package test;

public const val TOP_LEVEL = "O"

public object A {
    public const val OBJECT = "K"
}

public class B {
    companion object {
        public const val COMPANION = "56"
    }
}

annotation class Ann(val value: String)
