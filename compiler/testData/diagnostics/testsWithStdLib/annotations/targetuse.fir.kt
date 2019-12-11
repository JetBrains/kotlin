// FILE: TargetUse.java

import java.lang.annotation.Target;

public class TargetUse {
    public static String foo(kotlin.annotation.Target aTarget) {
        return aTarget.toString();
    }

    public static String bar(Target aTarget) {
        return aTarget.toString();
    }
}

// FILE: TargetUse.kt

import java.lang.annotation.Target as JTarget

fun fooUse(aTarget: Target): String = TargetUse.foo(aTarget)

fun barUse(aTarget: JTarget): String = TargetUse.bar(aTarget)
