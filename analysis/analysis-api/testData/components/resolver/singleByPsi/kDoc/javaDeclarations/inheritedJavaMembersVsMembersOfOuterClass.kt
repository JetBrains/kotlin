// FILE: main.kt
import dependency.JavaBase

interface Outer1 {
    fun getAge(): Int
    fun getName(): String

    interface Outer2 {
        val age: Int
        val name: String

        class KotlinChild : JavaBase() {
            /**
             * [ag<caret_1>e]
             * [na<caret_2>me]
             * [getA<caret_3>ge]
             * [ge<caret_4>tName]
             */
            fun usage() {
                getName()
            }
        }
    }
}

// FILE: dependency/JavaBase.java
package dependency;

public class JavaBase {
    public int age;
    public String name;

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }
}