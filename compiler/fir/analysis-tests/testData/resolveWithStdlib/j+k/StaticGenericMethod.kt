// FILE: StaticOwner.java
// FULL_JDK

import org.jetbrains.annotations.NotNull;

public class StaticOwner {
    @NotNull
    public static <T> T newInstance(@NotNull Class<T> aClass) {}
}

// FILE: User.kt

interface Freezable

abstract class User<T : Freezable> {

    private var settings: T = createSettings()

    protected abstract fun createSettings(): T

    fun foo() {
        settings = StaticOwner.newInstance(settings.javaClass)
    }
}