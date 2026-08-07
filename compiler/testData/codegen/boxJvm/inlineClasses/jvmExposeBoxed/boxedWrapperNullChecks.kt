// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// A boxed wrapper must null-check its non-null parameters before unboxing them, so that a Java caller passing
// null gets the same "Parameter specified as non-null is null" failure it would get from any other Kotlin
// declaration, rather than a raw NullPointerException from inside the callee.

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

@JvmExposeBoxed
fun topLevel(id: Id, control: String): String = id.value + control

@JvmExposeBoxed
class Holder @JvmExposeBoxed constructor(val id: Id) {
    @JvmExposeBoxed
    fun member(other: Id): String = id.value + other.value
}

// FILE: Main.java
public class Main {
    public String topLevelNullValueClass() {
        try {
            ICKt.topLevel(null, "");
            return "no exception";
        } catch (NullPointerException e) {
            return name(e);
        }
    }

    public String topLevelNullControl() {
        try {
            ICKt.topLevel(new Id("O"), null);
            return "no exception";
        } catch (NullPointerException e) {
            return name(e);
        }
    }

    public String constructorNull() {
        try {
            new Holder(null);
            return "no exception";
        } catch (NullPointerException e) {
            return name(e);
        }
    }

    public String memberNull() {
        try {
            new Holder(new Id("O")).member(null);
            return "no exception";
        } catch (NullPointerException e) {
            return name(e);
        }
    }

    private static String name(NullPointerException e) {
        String message = e.getMessage();
        return message != null && message.contains("Parameter specified as non-null is null")
                ? "checked"
                : "unchecked";
    }
}

// FILE: Box.kt
fun box(): String {
    // The ordinary reference parameter is the control: it is null-checked today.
    val control = Main().topLevelNullControl()
    if (control != "checked") return "FAIL control: $control"

    val topLevel = Main().topLevelNullValueClass()
    if (topLevel != "checked") return "FAIL 1: $topLevel"

    val constructor = Main().constructorNull()
    if (constructor != "checked") return "FAIL 2: $constructor"

    val member = Main().memberNull()
    if (member != "checked") return "FAIL 3: $member"

    return "OK"
}
