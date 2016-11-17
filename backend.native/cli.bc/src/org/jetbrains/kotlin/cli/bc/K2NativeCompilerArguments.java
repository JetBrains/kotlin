package org.jetbrains.kotlin.cli.bc;

import com.sampullara.cli.Argument;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.ValueDescription;


public class K2NativeCompilerArguments extends CommonCompilerArguments {
    @Argument(value = "output", description = "Output file path")
    @ValueDescription("<path>")
    public String outputFile;

    @Argument(value = "runtime", description = "Runtime file path")
    @ValueDescription("<path>")
    public String runtimeFile;

    @Argument(value = "library", description = "Bitcode file with metadata attached")
    @ValueDescription("<path>")
    public String[] libraries;


    public K2NativeCompilerArguments() {
    }

    public K2NativeCompilerArguments(K2NativeCompilerArguments arguments) {
        this.outputFile = arguments.outputFile;
        this.runtimeFile = arguments.runtimeFile;
        this.libraries = arguments.libraries;
    }

    public CommonCompilerArguments copy() {
        return new K2NativeCompilerArguments(this);
    }
}
