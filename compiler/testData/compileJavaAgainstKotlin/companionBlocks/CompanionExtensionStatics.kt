// LANGUAGE: +CompanionBlocks +CompanionExtensions
// TARGET_BACKEND: JVM

// FILE: CompanionExtensionStatics.java
package test;

class JavaClient {
    public static void test() {
        // Companion extensions are statics of the file facade without a receiver parameter.
        Target average = CompanionExtensionStaticsKt.average(new Target(), new Target());
        CompanionExtensionStaticsKt.setCounter(CompanionExtensionStaticsKt.getCounter() + 1);
        Target absolute = CompanionExtensionStaticsKt.Absolute;

        // Same name on different receivers, separated with @JvmName.
        String cat = CompanionExtensionStaticsKt.speakCat();
        String dog = CompanionExtensionStaticsKt.speakDog();

        // Receiver of a Java type.
        StringBuilder builder = CompanionExtensionStaticsKt.ofWords("a", "b");
    }
}

// FILE: CompanionExtensionStatics.kt
package test

class Target
class Cat
class Dog

companion fun Target.average(a: Target, b: Target): Target = a
companion var Target.counter: Int = 0
@JvmField companion val Target.Absolute: Target = Target()

@JvmName("speakCat")
companion fun Cat.speak() = "meow"

@JvmName("speakDog")
companion fun Dog.speak() = "woof"

companion fun StringBuilder.ofWords(vararg words: String): StringBuilder = StringBuilder(words.joinToString(" "))
