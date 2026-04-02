// WithPlatformTypes
// LIBRARY_PLATFORMS: JVM

// FILE: p/J.java
package p;

import org.jetbrains.annotations.NotNull;

public interface J {
    @NotNull
    String foo(@NotNull String b);
}

// FILE: k.kt

import p.*

class WithPlatformTypes(j: J) : J by j