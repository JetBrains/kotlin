// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME
// FILE: Test.java

class Test {
    String test() {
        String s;

        s = Klass.NAME;
        if (!s.equals("Klass")) throw new AssertionError("Fail class: " + s);

        s = Klass.JVM_NAME;
        if (!s.equals("JvmKlass")) throw new AssertionError("Fail jvm class: " + s);

        s = Trait.NAME;
        if (!s.equals("Trait")) throw new AssertionError("Fail interface: " + s);

        s = Enoom.NAME;
        if (!s.equals("Enum")) throw new AssertionError("Fail enum: " + s);

        s = Enoom.JVM_NAME;
        if (!s.equals("JvmEnum")) throw new AssertionError("Fail jvm enum: " + s);

        return "OK";
    }
}

// FILE: test.kt

class Klass {
    companion object {
        const val NAME = "Klass"
        @JvmField val JVM_NAME = "JvmKlass"
    }
}

interface Trait {
    companion object {
        const val NAME = "Trait"
    }
}

enum class Enoom {
    ;
    companion object {
        const val NAME = "Enum"
        @JvmField val JVM_NAME = "JvmEnum"
    }
}

fun box() = Test().test()
