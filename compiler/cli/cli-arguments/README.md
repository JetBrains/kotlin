# Kotlin Compiler CLI Arguments

The compiler argument classes (`CommonCompilerArguments`, `K2JVMCompilerArguments`, …), their parser and the `CompilerMessageSeverity`
enum. Most of the classes are generated from the argument descriptions in `:compiler:arguments` by `:compiler:cli:cli-arguments-generator`.

The module is published in the `kotlin-analysis-api-implementation-cli-options` artifact, which has to work with the Analysis API surface
alone. So the module must not depend on the rest of the compiler: everything that turns the arguments into a compiler configuration, such
as the configurators and `toLanguageVersionSettings`, lives in `:compiler:cli-base`.
