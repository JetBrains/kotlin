// TARGET_BACKEND: JVM
// FILE: MySettings.java

import java.util.Collection;
import java.util.Collections;

public class MySettings<
        SS extends MySettings<SS, PS, L>,
        PS extends MyComparableSettings,
        L extends MySettingsListener<PS>
    >
{
    public Collection<PS> getLinkedProjectsSettings() {
        return Collections.emptyList();
    }

    public static MySettings getSettings() {
        return new MySettings();
    }
}

abstract class MyComparableSettings implements Comparable<MyComparableSettings> {}
abstract class MySettingsListener<S extends MyComparableSettings> {}

// FILE: test.kt

fun box(): String {
    val a = MySettings.getSettings()
    a.getLinkedProjectsSettings()
    a.linkedProjectsSettings

    return "OK"
}
