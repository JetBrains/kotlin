// !WITH_NEW_INFERENCE
// SKIP_JAVAC

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

abstract class MyComparableSettings implements Comparable<MyComparableSettings> {}
abstract class MySettingsListener<S extends MyComparableSettings> {}

// FILE: test.kt

fun test() {
    val a = MySettings.getSettings()
    a.getLinkedProjectsSettings()
    a.<!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>linkedProjectsSettings<!>
}