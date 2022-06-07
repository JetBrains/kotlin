// TARGET_BACKEND: JVM
// FIR_IDENTICAL
// ISSUE: KT-52676

// FILE: Keyboard.java

public class Keyboard {
    public static final int CHAR_NONE = 1234;
}

// FILE: main.kt

annotation class ModuleInfo(val keyBind: Int = Keyboard.CHAR_NONE)