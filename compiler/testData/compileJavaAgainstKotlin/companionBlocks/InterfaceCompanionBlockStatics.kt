// LANGUAGE: +CompanionBlocks
// TARGET_BACKEND: JVM

// FILE: InterfaceCompanionBlockStatics.java
package test;

import java.util.List;

class JavaClient {
    public static void test() {
        // Interface companion block members are static methods of the interface.
        List<String> all = Registry.all();
        Registry.setSize(Registry.getSize() + 1);

        // @JvmField properties are public static final fields of the interface.
        String name = Constants.NAME;
    }
}

// Interface statics are not inherited by implementing classes, so Java can declare an instance member with the same name.
class JavaRegistry implements Registry {
    public List<String> all() {
        return null;
    }
}

// FILE: InterfaceCompanionBlockStatics.kt
package test

interface Registry {
    companion {
        var size = 0

        fun all(): List<String> = listOf("a")
    }
}

interface Constants {
    companion {
        @JvmField val NAME = "constants"
    }
}
