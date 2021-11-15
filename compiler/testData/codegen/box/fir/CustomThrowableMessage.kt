// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

// MODULE: m1
// FILE: m1/ConfigurationException.java

package m1;

public class ConfigurationException extends Exception {
    public ConfigurationException(String message) {
        super(message);
    }

    public String getMessage() {
        return super.getMessage();
    }
}

// MODULE: m2(m1)
// FILE: m2/RuntimeConfigurationException.java

package m2;
import m1.ConfigurationException;

public class RuntimeConfigurationException extends ConfigurationException {
    public RuntimeConfigurationException(String message) {
        super(message);
    }
}

// FILE: m2/RuntimeConfigurationError.java

package m2;

public class RuntimeConfigurationError extends RuntimeConfigurationException{
    public RuntimeConfigurationError(String message) {
        super(message);
    }
}

// MODULE: m3(m2, m1)
// FILE: m3.kt

package m3
import m2.RuntimeConfigurationError

fun box(): String {
    try {
        throw RuntimeConfigurationError("OK")
    } catch (e: RuntimeConfigurationError) {
        return e.message!!
    }
}
