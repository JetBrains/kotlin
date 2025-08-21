/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.intellij.execution.configurations;

import com.intellij.diagnostic.LoadingState;
import com.intellij.execution.CommandLineUtil;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.IllegalEnvVarException;
import com.intellij.execution.Platform;
import com.intellij.execution.process.ProcessNotCreatedException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.containers.CollectionFactory;
import com.intellij.util.containers.FastUtilHashingStrategies;
import com.intellij.util.execution.ParametersListUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class GeneralCommandLine implements UserDataHolder {
    private static final Logger LOG = Logger.getInstance(GeneralCommandLine.class);

    /**
     * Determines the scope of a parent environment passed to a child process.
     * <p>
     * {@code NONE} means a child process will receive an empty environment. <br/>
     * {@code SYSTEM} will provide it with the same environment as an IDE. <br/>
     * {@code CONSOLE} provides the child with a similar environment as if it was launched from, well, a console.
     * On OS X, a console environment is simulated (see {@link EnvironmentUtil#getEnvironmentMap()} for reasons it's needed
     * and details on how it works). On Windows and Unix hosts, this option is no different from {@code SYSTEM}
     * since there is no drastic distinction in environment between GUI and console apps.
     */
    public enum ParentEnvironmentType {NONE, SYSTEM, CONSOLE}

    private String myExePath;
    private File myWorkDirectory;
    private final Map<String, String> myEnvParams = new MyMap();
    private ParentEnvironmentType myParentEnvironmentType = ParentEnvironmentType.CONSOLE;
    private final ParametersList myProgramParams = new ParametersList();
    private Charset myCharset = defaultCharset();
    private boolean myRedirectErrorStream;
    private File myInputFile;
    private Map<Object, Object> myUserData;

    public GeneralCommandLine() { }

    public GeneralCommandLine(@NonNls String... command) {
        this(Arrays.asList(command));
    }

    public GeneralCommandLine(@NonNls @NotNull List<String> command) {
        int size = command.size();
        if (size > 0) {
            setExePath(command.get(0));
            if (size > 1) {
                addParameters(command.subList(1, size));
            }
        }
    }

    protected GeneralCommandLine(@NotNull GeneralCommandLine original) {
        myExePath = original.myExePath;
        myWorkDirectory = original.myWorkDirectory;
        myEnvParams.putAll(original.myEnvParams);
        myParentEnvironmentType = original.myParentEnvironmentType;
        original.myProgramParams.copyTo(myProgramParams);
        myCharset = original.myCharset;
        myRedirectErrorStream = original.myRedirectErrorStream;
        myInputFile = original.myInputFile;
        myUserData = null;  // user data should not be copied over
    }

    private static Charset defaultCharset() {
        return LoadingState.COMPONENTS_LOADED.isOccurred() ? EncodingManager.getInstance().getDefaultConsoleEncoding() : Charset.defaultCharset();
    }

    public @NotNull @NlsSafe String getExePath() {
        return myExePath;
    }

    public @NotNull GeneralCommandLine withExePath(@NotNull @NlsSafe String exePath) {
        myExePath = exePath.trim();
        return this;
    }

    public void setExePath(@NotNull @NlsSafe String exePath) {
        withExePath(exePath);
    }

    public File getWorkDirectory() {
        return myWorkDirectory;
    }

    @NotNull
    public GeneralCommandLine withWorkDirectory(@Nullable String path) {
        return withWorkDirectory(path != null ? new File(path) : null);
    }

    @NotNull
    public GeneralCommandLine withWorkDirectory(@Nullable File workDirectory) {
        myWorkDirectory = workDirectory;
        return this;
    }

    public void setWorkDirectory(@Nullable String path) {
        withWorkDirectory(path);
    }

    public void setWorkDirectory(@Nullable File workDirectory) {
        withWorkDirectory(workDirectory);
    }

    /**
     * Note: the map returned is forgiving to passing null values into putAll().
     */
    @NotNull
    public Map<String, String> getEnvironment() {
        return myEnvParams;
    }

    @NotNull
    public GeneralCommandLine withEnvironment(@Nullable Map<String, String> environment) {
        if (environment != null) {
            getEnvironment().putAll(environment);
        }
        return this;
    }

    @NotNull
    public GeneralCommandLine withEnvironment(@NonNls @NotNull String key, @NonNls @NotNull String value) {
        getEnvironment().put(key, value);
        return this;
    }

    public boolean isPassParentEnvironment() {
        return myParentEnvironmentType != ParentEnvironmentType.NONE;
    }

    /** @deprecated use {@link #withParentEnvironmentType(ParentEnvironmentType)} */
    @Deprecated
    public void setPassParentEnvironment(boolean passParentEnvironment) {
        withParentEnvironmentType(passParentEnvironment ? ParentEnvironmentType.CONSOLE : ParentEnvironmentType.NONE);
    }

    @NotNull
    public ParentEnvironmentType getParentEnvironmentType() {
        return myParentEnvironmentType;
    }

    @NotNull
    public GeneralCommandLine withParentEnvironmentType(@NotNull ParentEnvironmentType type) {
        myParentEnvironmentType = type;
        return this;
    }

    /**
     * Returns an environment that will be inherited by a child process.
     * @see #getEffectiveEnvironment()
     */
    @NotNull
    public Map<String, String> getParentEnvironment() {
        switch (myParentEnvironmentType) {
            case SYSTEM:
                return System.getenv();
            case CONSOLE:
                return EnvironmentUtil.getEnvironmentMap();
            default:
                return Collections.emptyMap();
        }
    }

    /**
     * Returns an environment as seen by a child process,
     * that is the {@link #getEnvironment() environment} merged with the {@link #getParentEnvironment() parent} one.
     */
    @NotNull
    public Map<String, String> getEffectiveEnvironment() {
        Map<String, String> env = new MyMap();
        setupEnvironment(env);
        return env;
    }

    public void addParameters(@NonNls String... parameters) {
        withParameters(parameters);
    }

    public void addParameters(@NotNull List<String> parameters) {
        withParameters(parameters);
    }

    @NotNull
    public GeneralCommandLine withParameters(@NotNull @NonNls String... parameters) {
        for (String parameter : parameters) addParameter(parameter);
        return this;
    }

    @NotNull
    public GeneralCommandLine withParameters(@NotNull List<String> parameters) {
        for (String parameter : parameters) addParameter(parameter);
        return this;
    }

    public void addParameter(@NonNls @NotNull String parameter) {
        myProgramParams.add(parameter);
    }

    @NotNull
    public ParametersList getParametersList() {
        return myProgramParams;
    }

    @NotNull
    public Charset getCharset() {
        return myCharset;
    }

    @NotNull
    public GeneralCommandLine withCharset(@NotNull Charset charset) {
        myCharset = charset;
        return this;
    }

    public void setCharset(@NotNull Charset charset) {
        withCharset(charset);
    }

    public boolean isRedirectErrorStream() {
        return myRedirectErrorStream;
    }

    @NotNull
    public GeneralCommandLine withRedirectErrorStream(boolean redirectErrorStream) {
        myRedirectErrorStream = redirectErrorStream;
        return this;
    }

    public void setRedirectErrorStream(boolean redirectErrorStream) {
        withRedirectErrorStream(redirectErrorStream);
    }

    public File getInputFile() {
        return myInputFile;
    }

    @NotNull
    public GeneralCommandLine withInput(@Nullable File file) {
        myInputFile = file;
        return this;
    }

    /**
     * Returns string representation of this command line.<br/>
     * Warning: resulting string is not OS-dependent - <b>do not</b> use it for executing this command line.
     *
     * @return single-string representation of this command line.
     */
    @NlsSafe
    @NotNull
    public String getCommandLineString() {
        return getCommandLineString(null);
    }

    /**
     * Returns string representation of this command line.<br/>
     * Warning: resulting string is not OS-dependent - <b>do not</b> use it for executing this command line.
     *
     * @param exeName use this executable name instead of given by {@link #setExePath(String)}
     * @return single-string representation of this command line.
     */
    @NotNull
    public String getCommandLineString(@Nullable String exeName) {
        return ParametersListUtil.join(getCommandLineList(exeName));
    }

    @NotNull
    public List<String> getCommandLineList(@Nullable String exeName) {
        List<@NlsSafe String> commands = new ArrayList<>();
        String exe = StringUtil.notNullize(exeName, StringUtil.notNullize(myExePath, "<null>"));
        commands.add(exe);

        commands.addAll(myProgramParams.getList());
        return commands;
    }

    /**
     * Prepares command (quotes and escapes all arguments) and returns it as a newline-separated list.
     *
     * @return command as a newline-separated list.
     * @see #getPreparedCommandLine(Platform)
     */
    @NotNull
    public String getPreparedCommandLine() {
        return getPreparedCommandLine(Platform.current());
    }

    /**
     * Prepares command (quotes and escapes all arguments) and returns it as a newline-separated list
     * (suitable e.g. for passing in an environment variable).
     *
     * @param platform a target platform
     * @return command as a newline-separated list.
     */
    @NotNull
    public String getPreparedCommandLine(@NotNull Platform platform) {
        String exePath = myExePath != null ? myExePath : "";
        return StringUtil.join(prepareCommandLine(exePath, myProgramParams.getList(), platform), "\n");
    }

    @NotNull
    protected List<String> prepareCommandLine(@NotNull String command, @NotNull List<String> parameters, @NotNull Platform platform) {
        return CommandLineUtil.toCommandLine(command, parameters, platform);
    }

    @NotNull
    public Process createProcess() throws ExecutionException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing [" + getCommandLineString() + "]");
            LOG.debug("  environment: " + myEnvParams + " (+" + myParentEnvironmentType + ")");
            LOG.debug("  charset: " + myCharset);
        }

        List<String> commands = validateAndPrepareCommandLine();
        try {
            return startProcess(commands);
        }
        catch (IOException e) {
            LOG.debug(e);
            throw new ProcessNotCreatedException(e.getMessage(), e, this);
        }
    }

    public @NotNull ProcessBuilder toProcessBuilder() throws ExecutionException {
        List<String> escapedCommands = validateAndPrepareCommandLine();
        return toProcessBuilderInternal(escapedCommands);
    }

    @NotNull
    private List<String> validateAndPrepareCommandLine() throws ExecutionException {
        try {
            if (myWorkDirectory != null) {
                if (!myWorkDirectory.exists()) {
                    throw new ExecutionException(myWorkDirectory + " does not exist");
                }
                if (!myWorkDirectory.isDirectory()) {
                    throw new ExecutionException(myWorkDirectory + " is not a directory");
                }
            }

            if (StringUtil.isEmptyOrSpaces(myExePath)) {
                throw new ExecutionException("executable not specified");
            }
        }
        catch (ExecutionException e) {
            LOG.debug(e);
            throw e;
        }

        for (Map.Entry<String, String> entry : myEnvParams.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (!EnvironmentUtil.isValidName(name)) throw new IllegalEnvVarException("run.configuration.invalid.env.name: " + name);
            if (!EnvironmentUtil.isValidValue(value)) throw new IllegalEnvVarException("run.configuration.invalid.env.value: " + name + ", " + value);
        }

        String exePath = myExePath;
        if (SystemInfoRt.isMac && myParentEnvironmentType == ParentEnvironmentType.CONSOLE && exePath.indexOf(File.separatorChar) == -1) {
            String systemPath = System.getenv("PATH");
            String shellPath = EnvironmentUtil.getValue("PATH");
            if (!Objects.equals(systemPath, shellPath)) {
                File exeFile = PathEnvironmentVariableUtil.findInPath(myExePath, shellPath, null);
                if (exeFile != null) {
                    LOG.debug(exePath + " => " + exeFile);
                    exePath = exeFile.getPath();
                }
            }
        }

        return prepareCommandLine(exePath, myProgramParams.getList(), Platform.current());
    }

    /**
     * @implNote for subclasses:
     * <p>On Windows the escapedCommands argument must never be modified or augmented in any way.
     * Windows command line handling is extremely fragile and vague, and the exact escaping of a particular argument may vary
     * depending on values of the preceding arguments.
     * <pre>
     *   [foo] [^] -> [foo] [^^]
     * </pre>
     * but:
     * <pre>
     *   [foo] ["] [^] -> [foo] [\"] ["^"]
     * </pre>
     * Notice how the last parameter escaping changes after prepending another argument.</p>
     * <p>If you need to alter the command line passed in, override the {@link #prepareCommandLine(String, List, Platform)} method instead.</p>
     */
    @NotNull
    protected Process startProcess(@NotNull List<String> escapedCommands) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Building process with commands: " + escapedCommands);
        }
        return toProcessBuilderInternal(escapedCommands).start();
    }

    // This is caused by the fact there are external usages overriding startProcess(List<String>).
    // Ideally, it should have been startProcess(ProcessBuilder), and the design would be more straightforward.
    @NotNull
    private ProcessBuilder toProcessBuilderInternal(@NotNull List<String> escapedCommands) {
        ProcessBuilder builder = new ProcessBuilder(escapedCommands);
        setupEnvironment(builder.environment());
        builder.directory(myWorkDirectory);
        builder.redirectErrorStream(myRedirectErrorStream);
        if (myInputFile != null) {
            builder.redirectInput(ProcessBuilder.Redirect.from(myInputFile));
        }
        return buildProcess(builder);
    }

    /**
     * Executed with pre-filled ProcessBuilder as the param and
     * gives the last chance to configure starting process
     * parameters before a process is started
     * @param builder filed ProcessBuilder
     */
    @NotNull
    protected ProcessBuilder buildProcess(@NotNull ProcessBuilder builder) {
        return builder;
    }

    protected void setupEnvironment(@NotNull Map<String, String> environment) {
        environment.clear();

        if (myParentEnvironmentType != ParentEnvironmentType.NONE) {
            environment.putAll(getParentEnvironment());
        }

        if (SystemInfoRt.isUnix) {
            File workDirectory = getWorkDirectory();
            if (workDirectory != null) {
                environment.put("PWD", FileUtil.toSystemDependentName(workDirectory.getAbsolutePath()));
            }
        }

        if (!myEnvParams.isEmpty()) {
            if (SystemInfoRt.isWindows) {
                Map<String, String> envVars = CollectionFactory.createCaseInsensitiveStringMap();
                envVars.putAll(environment);
                envVars.putAll(myEnvParams);
                environment.clear();
                environment.putAll(envVars);
            }
            else {
                environment.putAll(myEnvParams);
            }
        }
    }

    /**
     * Normally, double quotes in parameters are escaped, so they arrive to a called program as-is.
     * But some commands (e.g. {@code 'cmd /c start "title" ...'}) should get their quotes non-escaped.
     * Wrapping a parameter by this method (instead of using quotes) will do exactly this.
     *
     * @see com.intellij.execution.util.ExecUtil#getTerminalCommand(String, String)
     */
    @NotNull
    public static String inescapableQuote(@NotNull String parameter) {
        return CommandLineUtil.specialQuote(parameter);
    }

    @Override
    public String toString() {
        return myExePath + " " + myProgramParams;
    }

    @Nullable
    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        if (myUserData == null) return null;
        @SuppressWarnings("unchecked") T t = (T)myUserData.get(key);
        return t;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
        if (myUserData == null) {
            if (value == null) return;
            myUserData = new HashMap<>();
        }
        myUserData.put(key, value);
    }

    private static final class MyMap extends Object2ObjectOpenCustomHashMap<String, String> {
        private MyMap() {
            super(FastUtilHashingStrategies.getStringStrategy(!SystemInfoRt.isWindows));
        }

        @Override
        public void putAll(Map<? extends String, ? extends String> map) {
            if (map != null) {
                super.putAll(map);
            }
        }
    }
}
