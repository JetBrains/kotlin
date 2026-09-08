// WITH_STDLIB
// DIAGNOSTICS: -UNUSED_PARAMETER
// Shaped after real Chromium code: org.chromium.base.CommandLine and org.chromium.base.Callback

// FILE: org/chromium/base/Callback.java

package org.chromium.base;

import org.chromium.build.annotations.NullMarked;
import org.chromium.build.annotations.NullUnmarked;
import org.chromium.build.annotations.Nullable;

@NullMarked
public interface Callback<T extends @Nullable Object> {
    void onResult(T result);

    @NullUnmarked
    static <T extends @Nullable Object> void runNullSafe(@Nullable Callback<T> callback, T object) {
        if (callback != null) callback.onResult(object);
    }
}

// FILE: org/chromium/base/CommandLine.java

package org.chromium.base;

import java.util.Map;

import org.chromium.build.annotations.NullMarked;
import org.chromium.build.annotations.Nullable;

@NullMarked
public class CommandLine {
    private @Nullable Map<String, String> mSwitches;

    public static CommandLine getInstance() { return new CommandLine(); }

    public static void init(String @Nullable [] args) {}

    public boolean hasSwitch(String switchString) { return false; }

    public @Nullable String getSwitchValue(String switchString) { return null; }

    public String getSwitchValue(String switchString, String defaultValue) { return defaultValue; }

    public void appendSwitchWithValue(String switchString, @Nullable String value) {}
}

// FILE: main.kt

import org.chromium.base.Callback
import org.chromium.base.CommandLine

fun commandLine() {
    val commandLine = CommandLine.getInstance()

    commandLine.hasSwitch("enable-features")
    commandLine.hasSwitch(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)

    commandLine.getSwitchValue("enable-features")?.length
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>commandLine.getSwitchValue("enable-features")<!>.length
    commandLine.getSwitchValue("enable-features", "default").length

    commandLine.appendSwitchWithValue("enable-features", null)
    commandLine.appendSwitchWithValue(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>, null)

    CommandLine.init(null)
    CommandLine.init(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>arrayOf("chrome", null)<!>)
}

fun callbacks(nonNullCallback: Callback<String>, nullableCallback: Callback<String?>) {
    nonNullCallback.onResult("")
    nonNullCallback.onResult(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)

    nullableCallback.onResult("")
    nullableCallback.onResult(null)

    Callback.runNullSafe(null, "")
    Callback.runNullSafe(nullableCallback, null)
}

class KotlinCallback : Callback<String> {
    override fun onResult(result: String) {}
}
