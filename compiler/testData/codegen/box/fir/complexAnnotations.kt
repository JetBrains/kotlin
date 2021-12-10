// TARGET_BACKEND: JVM_IR

// MODULE: m1
// FILE: State.java
public @interface State {
    String name();

    Storage[] storages() default {};

    boolean reloadable() default true;

    boolean defaultStateAsResource() default false;

    boolean reportStatistic() default false;
}

// FILE: Storage.java
public @interface Storage {
    String file() default "";

    String value() default "";

    boolean deprecated() default false;

    RoamingType roamingType() default RoamingType.DEFAULT;
}

// FILE: RoamingType.java
public enum RoamingType {
    DISABLED,
    PER_OS,
    DEFAULT,
}

// FILE: StoragePathMacros.java
public class StoragePathMacros {
    public static final String NON_ROAMABLE_FILE = "NON_ROAMABLE_FILE";
}

// MODULE: m2(m1)
// FILE: test.kt

@State(name = "RecentDirectoryProjectsManager",
       storages = [Storage(value = "recentProjectDirectories.xml", roamingType = RoamingType.DISABLED, deprecated = true)],
       reportStatistic = false)
class Some

@State(name = "RecentProjectsManager", storages = [Storage(value = "recentProjects.xml", roamingType = RoamingType.DISABLED)])
class Other

@State(name = "A", storages = [(Storage(value = StoragePathMacros.NON_ROAMABLE_FILE))])
class Another

fun box(): String {
    Some()
    Other()
    Another()
    run {
        @State(name = "A", storages = [(Storage(value = StoragePathMacros.NON_ROAMABLE_FILE))])
        class Local
        Local()
    }
    return "OK"
}
