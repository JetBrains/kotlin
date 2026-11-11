// LANGUAGE: +CompanionBlocks
// TARGET_BACKEND: JVM

// FILE: CompanionBlockStatics.java
package test;

import java.io.IOException;

class JavaClient {
    public static void test() {
        Temperature.setCreated(Temperature.getCreated() + 1);
        Temperature boiling = Temperature.Boiling;

        Temperature parsed = Temperature.parseCelsius("36.6");
        String a = Temperature.make();
        String b = Temperature.make(1.0);

        try {
            Temperature.load();
        } catch (IOException e) {
            // @Throws is visible to javac
        }

        // Statics are available from subclass in Java.
        String unitThroughSubclass = JavaShape.unit();
    }
}

class JavaShape extends Shape {}

// FILE: CompanionBlockStatics.kt
package test

import java.io.IOException

class Temperature(val celsius: Double) {
    companion {
        var created = 0
        @JvmField val Boiling = Temperature(100.0)

        @JvmName("parseCelsius")
        fun parse(s: String) = Temperature(s.toDouble())

        @JvmOverloads
        fun make(celsius: Double = 0.0) = "$celsius"

        @Throws(IOException::class)
        fun load(): Temperature = throw IOException()
    }
}

open class Shape {
    companion {
        fun unit(): String = "unit"
    }
}
