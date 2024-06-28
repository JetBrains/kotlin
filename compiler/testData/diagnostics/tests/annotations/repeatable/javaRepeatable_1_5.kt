// FIR_IDENTICAL
// LANGUAGE: -RepeatableAnnotations
// FULL_JDK
// FILE: Runtime.java

import java.lang.annotation.*;

@Repeatable(Runtime.Container.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Runtime {
    public @interface Container {
        Runtime[] value();
    }
}

// FILE: Clazz.java

import java.lang.annotation.*;

@Repeatable(Clazz.Container.class)
@Retention(RetentionPolicy.CLASS)
public @interface Clazz {
    public @interface Container {
        Clazz[] value();
    }
}

// FILE: Source.java

import java.lang.annotation.*;

@Repeatable(Source.Container.class)
@Retention(RetentionPolicy.SOURCE)
public @interface Source {
    public @interface Container {
        Source[] value();
    }
}

// FILE: usage.kt

@Runtime <!REPEATED_ANNOTATION!>@Runtime<!>
class UseRuntime

@Clazz <!REPEATED_ANNOTATION!>@Clazz<!>
class UseClazz

@Source @Source
class UseSource
