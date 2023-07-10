// !CHECK_TYPE
// FILE: PythonRunParams.java
import java.util.Map;

public interface PythonRunParams {
    Map<String, String> fetchEnvs();
}

// FILE: AbstractPythonRunConfiguration.java
import java.util.HashMap;
import java.util.Map;

public class AbstractPythonRunConfiguration<T> implements PythonRunParams {
    public Map<String, String> fetchEnvs() {
        return null;
    }
}


// FILE: PythonRunConfiguration.java
public class PythonRunConfiguration extends AbstractPythonRunConfiguration implements PythonRunParams {}

// FILE: ProjectMain.kt

fun getRunCommandLine(configuration: PythonRunConfiguration) {
    fun foo(envs: Map<String, String>) {}
    checkSubtype<Map<String, String>>(configuration.fetchEnvs())
    checkSubtype<Map<*, *>>(configuration.fetchEnvs())
}
