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

    @Argument(value = "headers", description = "Header files used for compilation")
    @ValueDescription("<path>")
    public String[] headers;

}
