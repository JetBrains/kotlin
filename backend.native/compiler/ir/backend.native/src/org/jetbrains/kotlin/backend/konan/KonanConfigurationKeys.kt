package org.jetbrains.kotlin.backend.konan;

import org.jetbrains.kotlin.config.CompilerConfigurationKey;
import org.jetbrains.kotlin.serialization.js.ModuleKind;

class KonanConfigKeys {
    companion object {
        val LIBRARY_FILES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("library file paths");
        val RUNTIME_FILE: CompilerConfigurationKey<String> 
                = CompilerConfigurationKey.create("runtime file path");
        val OUTPUT_FILE: CompilerConfigurationKey<String> 
                = CompilerConfigurationKey.create("output file path");
        val ABI_VERSION: CompilerConfigurationKey<Int> 
                = CompilerConfigurationKey.create("current abi version");

        val SOURCE_MAP: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("generate source map");
        val META_INFO: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("generate metadata");
        val MODULE_KIND: CompilerConfigurationKey<ModuleKind> 
                = CompilerConfigurationKey.create("module kind");

        val VERIFY_IR: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("verify ir");
        val VERIFY_DESCRIPTORS: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("verify descriptors");
        val VERIFY_BITCODE: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("verify bitcode");

        val PRINT_IR: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("print ir");
        val PRINT_DESCRIPTORS: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("print descriptors");
        val PRINT_BITCODE: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("print bitcode");

        val ENABLED_PHASES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("enable backend phases");
        val DISABLED_PHASES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("disable backend phases");
        val VERBOSE_PHASES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("verbose backend phases");
        val LIST_PHASES: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("list backend phases");
    }
}

