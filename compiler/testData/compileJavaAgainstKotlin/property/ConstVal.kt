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
