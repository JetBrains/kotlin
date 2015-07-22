// FILE: TargetUse.java

import kotlin.annotation.target;
import java.lang.annotation.Target;

public class TargetUse {
    public static String foo(target aTarget) {
        return aTarget.toString();
    }

    public static String bar(Target aTarget) {
        return aTarget.toString();
    }
}

// FILE: TargetUse.kt

import java.lang.annotation.Target

fun fooUse(aTarget: target): String = TargetUse.foo(aTarget)

fun barUse(aTarget: Target): String = TargetUse.bar(aTarget)
