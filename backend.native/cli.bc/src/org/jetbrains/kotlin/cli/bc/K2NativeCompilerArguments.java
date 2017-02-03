package org.jetbrains.kotlin.cli.bc;

import com.sampullara.cli.Argument;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.ValueDescription;

public class K2NativeCompilerArguments extends CommonCompilerArguments {
    @Argument(value = "output", alias = "o", description = "Output file path")
    @ValueDescription("<path>")
    public String outputFile;

    @Argument(value = "runtime", description = "Override standard \'runtime.bc\' location")
    @ValueDescription("<path>")
    public String runtimeFile;

    @Argument(value = "properties", description = "Override standard \'konan.properties\' location")
    @ValueDescription("<path>")
    public String propertyFile;

    @Argument(value = "library", alias = "l", description = "Link with the library")
    @ValueDescription("<path>")
    public String[] libraries;

    @Argument(value = "nativelibrary", alias = "nl", description = "Link with the native library")
    @ValueDescription("<path>")
    public String[] nativeLibraries;

    @Argument(value = "nolink", description = "Don't link, just produce a bitcode file")
    public boolean nolink;

    @Argument(value = "nostdlib", description = "Don't link with stdlib")
    public boolean nostdlib;

    @Argument(value = "opt", description = "Enable optimizations during compilation")
    public boolean optimization;

    @Argument(value = "target", description = "Set hardware target")
    @ValueDescription("<target>")
    public String target;

    @Argument(value = "list_targets", description = "List available hardware targets")
    public boolean listTargets;

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

    @Argument(value = "time", description = "Report execution time for compiler phases")
    public boolean timePhases;
}

