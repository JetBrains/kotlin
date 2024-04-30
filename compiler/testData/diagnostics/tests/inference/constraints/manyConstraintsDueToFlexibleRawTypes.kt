// FIR_IDENTICAL
// SKIP_JAVAC

// FILE: MyComparableSettings.java
abstract class MyComparableSettings implements Comparable<MyComparableSettings> {}

// FILE: MySettingsListener.java
abstract class MySettingsListener<S extends MyComparableSettings> {}

// FILE: MySettings.java

import java.util.Collection;

public class MySettings<
        SS extends MySettings<SS, PS, L>,
        PS extends MyComparableSettings,
        L extends MySettingsListener<PS>
    >
{
    public Collection<PS> getLinkedProjectsSettings() {
        return null;
    }

    public static MySettings getSettings() {
        return null;
    }
}

// FILE: test.kt

fun test() {
    val a = MySettings.getSettings()
    a.getLinkedProjectsSettings()
    a.linkedProjectsSettings
}
