// FILE: Build.java
package os;

public class Build {
    public static class VERSION_CODES {
        public static final int CUPCAKE = 3;
    }
}

// FILE: main.kt
import os.Build

annotation class Annotation(val api: Int)

@get:Annotation(api = <expr>Build.VERSION_CODES.CUPCAKE</expr>)
val versionCheck1: Boolean
  get() = false