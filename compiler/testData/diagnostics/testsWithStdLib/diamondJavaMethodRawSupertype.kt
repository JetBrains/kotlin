// FIR_IDENTICAL
// ISSUE: KT-57202

// FILE: PythonRunParams.java

import java.util.Map;

public interface PythonRunParams {
    Map<String, String> fetchEnvs();
}

// FILE: AbstractPythonRunConfiguration.java

import java.util.Map;

public class AbstractPythonRunConfiguration<T> implements PythonRunParams {
    public Map<String, String> fetchEnvs() { return null; }
}

// FILE: PythonRunConfiguration.java

public class PythonRunConfiguration extends AbstractPythonRunConfiguration implements PythonRunParams {}

// FILE: ProjectMain.kt

fun getRunCommandLine(configuration: PythonRunConfiguration) {
    fun foo(envs: Map<String, String>) {}
    foo(configuration.fetchEnvs()) // K1: ok, K2: ARGUMENT_TYPE_MISMATCH
}
