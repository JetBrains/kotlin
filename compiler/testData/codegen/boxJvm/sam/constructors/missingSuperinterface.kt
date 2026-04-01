// TARGET_BACKEND: JVM
// MODULE: dep
// FILE: dep/Callback.java

package dep;

public interface Callback {}

// MODULE: lib(dep)
// FILE: lib/ThemeCallback.java

package lib;

import dep.Callback;

public interface ThemeCallback extends Callback {
    void callback(boolean isDark);
}

// MODULE: app(lib)
// FILE: main.kt

import lib.ThemeCallback

interface ThemeManager {
    val nativeThemeCallback: ThemeCallback
}

class FrontendThemeManager : ThemeManager {
    override val nativeThemeCallback = ThemeCallback {}
}

fun box(): String {
    FrontendThemeManager().nativeThemeCallback
    return "OK"
}
