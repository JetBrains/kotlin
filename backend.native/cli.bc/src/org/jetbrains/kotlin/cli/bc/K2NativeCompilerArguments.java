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

    @Argument(value = "print_ir", description = "Print IR")
    public boolean printIr;

    @Argument(value = "print_descriptors", description = "Print descriptor tree")
    public boolean printDescriptors;

    @Argument(value = "print_bitcode", description = "Print llvm bitcode")
    public boolean printBitCode;

    @Argument(value = "verify_ir", description = "Verify IR")
    public boolean verifyIr;

    @Argument(value = "verify_descriptors", description = "Verify descriptor tree")
    public boolean verifyDescriptors;

    @Argument(value = "verify_bitcode", description = "Verify llvm bitcode")
    public boolean verifyBitCode;

    @Argument(value = "enable", description = "Enable backend phase")
    @ValueDescription("<Phase>")
    public String[] enablePhases;

    @Argument(value = "disable", description = "Disable backend phase")
    @ValueDescription("<Phase>")
    public String[] disablePhases;

    @Argument(value = "verbose", description = "Trace phase execution")
    @ValueDescription("<Phase>")
    public String[] verbosePhases;

    @Argument(value = "list", description = "List all backend phases")
    public boolean listPhases;
}

