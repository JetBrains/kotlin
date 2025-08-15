/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.dsl.types.JvmTarget
import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.base.ReleaseDependent
import org.jetbrains.kotlin.arguments.dsl.defaultFalse
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.defaultOne
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.arguments.dsl.types.IntType
import org.jetbrains.kotlin.arguments.dsl.types.KotlinJvmTargetType
import org.jetbrains.kotlin.arguments.dsl.types.StringArrayType
import org.jetbrains.kotlin.arguments.dsl.types.StringType
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue
import org.jetbrains.kotlin.cli.common.arguments.Enables
import org.jetbrains.kotlin.cli.common.arguments.GradleInputTypes
import org.jetbrains.kotlin.cli.common.arguments.GradleOption
import org.jetbrains.kotlin.config.LanguageFeature

val actualJvmCompilerArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.jvmCompilerArguments) {
    compilerArgument {
        name = "d"
        compilerName = "destination"
        description = "Destination for generated class files.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<directory|jar>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
            stabilizedVersion = KotlinReleaseVersion.v1_0_0,
        )
    }

    compilerArgument {
        name = "classpath"
        shortName = "cp"
        description = "List of directories and JAR/ZIP archives to search for user class files.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
            stabilizedVersion = KotlinReleaseVersion.v1_0_0,
        )
    }

    compilerArgument {
        name = "include-runtime"
        description = "Include the Kotlin runtime in the resulting JAR.".asReleaseDependent()
        valueType = BooleanType.defaultFalse


        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
            stabilizedVersion = KotlinReleaseVersion.v1_0_0,
        )
    }

    compilerArgument {
        name = "jdk-home"
        description = "Include a custom JDK from the specified location in the classpath instead of the default 'JAVA_HOME'.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_3,
            stabilizedVersion = KotlinReleaseVersion.v1_0_3,
        )
    }

    compilerArgument {
        name = "no-jdk"
        description = "Don't automatically include the Java runtime in the classpath.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalMetadata(
            GradleOption(
                value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
                gradleInputType = GradleInputTypes.INPUT,
                shouldGenerateDeprecatedKotlinOptions = true,
            )
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
            stabilizedVersion = KotlinReleaseVersion.v1_0_0,
        )
    }

    compilerArgument {
        name = "no-stdlib"
        description = "Don't automatically include the Kotlin/JVM stdlib and Kotlin reflection dependencies in the classpath.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
            stabilizedVersion = KotlinReleaseVersion.v1_0_0,
        )
    }

    compilerArgument {
        name = "no-reflect"
        description = "Don't automatically include the Kotlin reflection dependency in the classpath.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_4,
            stabilizedVersion = KotlinReleaseVersion.v1_0_4,
        )
    }

    compilerArgument {
        name = "expression"
        shortName = "e"
        description = "Evaluate the given string as a Kotlin script.".asReleaseDependent()
        valueType = StringType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
            stabilizedVersion = KotlinReleaseVersion.v1_4_0,
        )
    }

    compilerArgument {
        name = "script-templates"
        description = "Script definition template classes.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<fully qualified class name[,]>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_0,
            stabilizedVersion = KotlinReleaseVersion.v1_1_0,
        )
    }

    compilerArgument {
        name = "module-name"
        description = "Name of the generated '.kotlin_module' file.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<name>".asReleaseDependent()

        additionalMetadata(
            GradleOption(
                value = DefaultValue.STRING_NULL_DEFAULT,
                gradleInputType = GradleInputTypes.INPUT,
                shouldGenerateDeprecatedKotlinOptions = true,
            )
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
            stabilizedVersion = KotlinReleaseVersion.v1_0_0,
        )
    }

    compilerArgument {
        name = "jvm-target"
        description = ReleaseDependent(
            current = "The target version of the generated JVM bytecode (${JvmTarget.CURRENT_SUPPORTED_VERSIONS_DESCRIPTION}), " +
                    "with ${JvmTarget.CURRENT_DEFAULT_VERSION} as the default."
        )

        valueType = KotlinJvmTargetType(
            isNullable = true.asReleaseDependent(),
            defaultValue = null.asReleaseDependent(),
        )
        valueDescription = "<version>".asReleaseDependent()

        additionalMetadata(
            GradleOption(
                value = DefaultValue.JVM_TARGET_VERSIONS,
                gradleInputType = GradleInputTypes.INPUT,
                shouldGenerateDeprecatedKotlinOptions = true,
            )
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_3,
            stabilizedVersion = KotlinReleaseVersion.v1_0_3,
        )
    }

    compilerArgument {
        name = "java-parameters"
        description = "Generate metadata for Java 1.8 reflection on method parameters.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalMetadata(
            GradleOption(
                value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
                gradleInputType = GradleInputTypes.INPUT,
                shouldGenerateDeprecatedKotlinOptions = true,
            )
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_0,
            stabilizedVersion = KotlinReleaseVersion.v1_1_0,
        )
    }

    compilerArgument {
        name = "jvm-default"
        compilerName = "jvmDefaultStable"
        description = """Emit JVM default methods for interface declarations with bodies. The default is 'enable'.
-jvm-default=enable              Generate default methods for non-abstract interface declarations, as well as 'DefaultImpls' classes with
                                 static methods for compatibility with code compiled in the 'disable' mode.
                                 This is the default behavior since language version 2.2.
-jvm-default=no-compatibility    Generate default methods for non-abstract interface declarations. Do not generate 'DefaultImpls' classes.
-jvm-default=disable             Do not generate JVM default methods. This is the default behavior up to language version 2.1.""".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{enable|no-compatibility|disable}".asReleaseDependent()

        additionalMetadata(
            GradleOption(
                value = DefaultValue.JVM_DEFAULT_MODES,
                gradleInputType = GradleInputTypes.INPUT,
                gradleName = "jvmDefault",
            )
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
            stabilizedVersion = KotlinReleaseVersion.v2_2_0,
        )
    }

    // Advanced options

    compilerArgument {
        name = "Xallow-unstable-dependencies"
        description = "Do not report errors on classes in dependencies that were compiled by an unstable version of the Kotlin compiler.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_30,
        )
    }

    compilerArgument {
        name = "Xabi-stability"
        description = """When using unstable compiler features such as FIR, use 'stable' to mark generated class files as stable
to prevent diagnostics from being reported when using stable compilers at the call site.
When using the JVM IR backend, conversely, use 'unstable' to mark generated class files as unstable
to force diagnostics to be reported.""".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{stable|unstable}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_30,
        )
    }

    compilerArgument {
        name = "Xir-do-not-clear-binding-context"
        compilerName = "doNotClearBindingContext"
        description = "When using the IR backend, do not clear BindingContext between 'psi2ir' and lowerings.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_30,
        )
    }

    compilerArgument {
        name = "Xbackend-threads"
        description = """Run codegen phase in N parallel threads.
0 means use one thread per processor core.
The default value is 1.""".asReleaseDependent()
        valueType = IntType.defaultOne
        valueDescription = "<N>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_6_20,
        )
    }

    compilerArgument {
        name = "Xmodule-path"
        compilerName = "javaModulePath"
        description = "Paths to Java 9+ modules.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_4,
        )
    }

    compilerArgument {
        name = "Xadd-modules"
        compilerName = "additionalJavaModules"
        description = "Root modules to resolve in addition to the initial modules, or all modules on the module path if <module> is ALL-MODULE-PATH.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<module[,]>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_4,
        )
    }

    compilerArgument {
        name = "Xno-call-assertions"
        description = "Don't generate not-null assertions for arguments of platform types.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
        )
    }

    compilerArgument {
        name = "Xno-receiver-assertions"
        description = "Don't generate not-null assertions for extension receiver arguments of platform types.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_50,
        )
    }

    compilerArgument {
        name = "Xno-param-assertions"
        description = "Don't generate not-null assertions on parameters of methods accessible from Java.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
        )
    }

    compilerArgument {
        name = "Xno-optimize"
        description = "Disable optimizations.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
        )
    }

    compilerArgument {
        name = "Xassertions"
        compilerName = "assertionsMode"
        description = """'kotlin.assert' call behavior:
-Xassertions=always-enable:  enable, ignore JVM assertion settings;
-Xassertions=always-disable: disable, ignore JVM assertion settings;
-Xassertions=jvm:            enable, depend on JVM assertion settings;
-Xassertions=legacy:         calculate the condition on each call, the behavior depends on JVM assertion settings in the kotlin package;
default: legacy""".asReleaseDependent()
        // TODO: change to JVMAssertionsMode type
        valueType = StringType(defaultValue = "legacy".asReleaseDependent())
        valueDescription = "{always-enable|always-disable|jvm|legacy}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_2_60,
        )
    }

    compilerArgument {
        name = "Xbuild-file"
        deprecatedName = "module"
        description = "Path to the .xml build file to compile.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_4,
        )
    }

    compilerArgument {
        name = "Xmultifile-parts-inherit"
        compilerName = "inheritMultifileParts"
        description = "Compile multifile classes as a hierarchy of parts and a facade.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_2,
        )
    }

    compilerArgument {
        name = "Xuse-type-table"
        description = "Use a type table in metadata serialization.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_2_40,
        )
    }

    compilerArgument {
        name = "Xuse-old-class-files-reading"
        description = """Use the old implementation for reading class files. This may slow down the compilation and cause problems with Groovy interop.
This can be used in the event of problems with the new implementation.""".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_3,
        )
    }

    compilerArgument {
        name = "Xuse-fast-jar-file-system"
        description = "Use the fast implementation of Jar FS. This may speed up compilation time, but it is experimental.".asReleaseDependent()
        valueType = BooleanType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_6_0,
        )
    }

    compilerArgument {
        name = "Xsuppress-missing-builtins-error"
        description = """Suppress the "cannot access built-in declaration" error (useful with '-no-stdlib').""".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_40,
        )
    }

    compilerArgument {
        name = "Xscript-resolver-environment"
        description = "Set the script resolver environment in key-value pairs (the value can be quoted and escaped).".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<key=value[,]>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_2,
        )
    }

    // Javac options

    compilerArgument {
        name = "Xuse-javac"
        description = "Use javac for Java source and class file analysis.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_4,
        )
    }

    compilerArgument {
        name = "Xcompile-java"
        description = "Reuse 'javac' analysis and compile Java source files.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_50,
        )
    }

    compilerArgument {
        name = "Xjavac-arguments"
        description = "Java compiler arguments.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<option[,]>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_4,
        )
    }

    compilerArgument {
        name = "Xjava-source-roots"
        description = "Paths to directories with Java source files.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_40,
        )
    }

    compilerArgument {
        name = "Xjava-package-prefix"
        description = "Package prefix for Java files.".asReleaseDependent()
        valueType = StringType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_40,
        )
    }

    compilerArgument {
        name = "Xjsr305"
        deprecatedName = "Xjsr305-annotations"
        description = """Specify the behavior of 'JSR-305' nullability annotations:
-Xjsr305={ignore/strict/warn}                   global (all non-@UnderMigration annotations)
-Xjsr305=under-migration:{ignore/strict/warn}   all @UnderMigration annotations
-Xjsr305=@<fq.name>:{ignore/strict/warn}        annotation with the given fully qualified class name
Modes:
* ignore
* strict (experimental; treat like other supported nullability annotations)
* warn (report a warning)""".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "{ignore/strict/warn}|under-migration:{ignore/strict/warn}|@<fq.name>:{ignore/strict/warn}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_50,
        )
    }

    compilerArgument {
        name = "Xnullability-annotations"
        description = """Specify the behavior for specific Java nullability annotations (provided with fully qualified package name).
Modes:
* ignore
* strict
* warn (report a warning)""".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "@<fq.name>:{ignore/strict/warn}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_30,
        )
    }

    compilerArgument {
        name = "Xsupport-compatqual-checker-framework-annotations"
        description = """Specify the behavior for Checker Framework 'compatqual' annotations ('NullableDecl'/'NonNullDecl').
The default value is 'enable'.""".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "enable|disable".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_2_20,
        )
    }

    compilerArgument {
        name = "Xjspecify-annotations"
        description = ReleaseDependent(
            """
                Specify the behavior of 'jspecify' annotations.
                The default value is 'strict'.""".trimIndent(),
            KotlinReleaseVersion.v1_4_30..KotlinReleaseVersion.v2_0_21 to
                    """
                       Specify the behavior of 'jspecify' annotations.
                       The default value is 'warn'.""".trimIndent()
        )
        valueType = StringType.defaultNull
        valueDescription = "ignore|strict|warn".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_30,
        )
    }

    compilerArgument {
        name = "Xjvm-default"
        description = """This option is deprecated. Migrate to -jvm-default as follows:
-Xjvm-default=disable            -> -jvm-default=disable
-Xjvm-default=all-compatibility  -> -jvm-default=enable
-Xjvm-default=all                -> -jvm-default=no-compatibility""".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{all|all-compatibility|disable}".asReleaseDependent()

        additionalAnnotations(
            Deprecated("This flag is deprecated. Use `-jvm-default` instead")
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_2_50,
            deprecatedVersion = KotlinReleaseVersion.v2_2_0,
        )
    }

    compilerArgument {
        name = "Xdefault-script-extension"
        description = "Compile expressions and unrecognized scripts passed with the -script argument as scripts with the given filename extension.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<script filename extension>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_30,
        )
    }

    compilerArgument {
        name = "Xdisable-standard-script"
        description = "Disable standard Kotlin scripting support.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_2_50,
        )
    }

    compilerArgument {
        name = "Xgenerate-strict-metadata-version"
        compilerName = "strictMetadataVersionSemantics"
        description = "Generate metadata with strict version semantics (see the KDoc entry on 'Metadata.extraInt').".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_0,
        )
    }

    compilerArgument {
        name = "Xsanitize-parentheses"
        description = """Transform '(' and ')' in method names to some other character sequence.
This mode can BREAK BINARY COMPATIBILITY and should only be used as a workaround for
problems with parentheses in identifiers on certain platforms.""".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_30,
        )
    }

    compilerArgument {
        name = "Xfriend-paths"
        description = "Paths to output directories for friend modules (modules whose internals should be visible).".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_2_70,
        )
    }

    compilerArgument {
        name = "Xallow-no-source-files"
        description = "Allow the set of source files to be empty.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_40,
        )
    }

    compilerArgument {
        name = "Xemit-jvm-type-annotations"
        description = "Emit JVM type annotations in bytecode.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_70,
        )
    }

    compilerArgument {
        name = "Xjvm-expose-boxed"
        description = "Expose inline classes and functions, accepting and returning them, to Java.".asReleaseDependent()
        valueType = BooleanType.defaultFalse
        additionalAnnotations(Enables(LanguageFeature.ImplicitJvmExposeBoxed))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
        )
    }

    compilerArgument {
        name = "Xstring-concat"
        description = """Select the code generation scheme for string concatenation:
-Xstring-concat=indy-with-constants  Concatenate strings using 'invokedynamic' and 'makeConcatWithConstants'. This requires '-jvm-target 9' or greater.
-Xstring-concat=indy                 Concatenate strings using 'invokedynamic' and 'makeConcat'. This requires '-jvm-target 9' or greater.
-Xstring-concat=inline               Concatenate strings using 'StringBuilder'
default: 'indy-with-constants' for JVM targets 9 or greater, 'inline' otherwise.""".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{indy-with-constants|indy|inline}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_20,
        )
    }

    compilerArgument {
        name = "Xjdk-release"
        description = """Compile against the specified JDK API version, similarly to javac's '-release'. This requires JDK 9 or newer.
The supported versions depend on the JDK used; for JDK 17+, the supported versions are ${JvmTarget.CURRENT_SUPPORTED_VERSIONS_DESCRIPTION}.
This also sets the value of '-jvm-target' to be equal to the selected JDK version.""".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<version>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
        )
    }

    compilerArgument {
        name = "Xsam-conversions"
        description = """Select the code generation scheme for SAM conversions.
-Xsam-conversions=indy          Generate SAM conversions using 'invokedynamic' with 'LambdaMetafactory.metafactory'.
-Xsam-conversions=class         Generate SAM conversions as explicit classes.
The default value is 'indy'.""".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{class|indy}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_0,
        )
    }

    compilerArgument {
        name = "Xlambdas"
        description = """Select the code generation scheme for lambdas.
-Xlambdas=indy                  Generate lambdas using 'invokedynamic' with 'LambdaMetafactory.metafactory'.
                                A lambda object created using 'LambdaMetafactory.metafactory' will have a different 'toString()'.
-Xlambdas=class                 Generate lambdas as explicit classes.
The default value is 'indy' if language version is 2.0+, and 'class' otherwise.""".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{class|indy}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_0,
        )
    }

    compilerArgument {
        name = "Xindy-allow-annotated-lambdas"
        description = "Allow using 'invokedynamic' for lambda expressions with annotations".asReleaseDependent()
        valueType = BooleanType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
        )
    }

    compilerArgument {
        name = "Xklib"
        compilerName = "klibLibraries"
        description = "Paths to cross-platform libraries in the .klib format.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
        )
    }

    compilerArgument {
        name = "Xno-reset-jar-timestamps"
        description = "Don't reset jar entry timestamps to a fixed date.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_30,
        )
    }

    compilerArgument {
        name = "Xno-unified-null-checks"
        description = "Use pre-1.4 exception types instead of 'java.lang.NPE' in null checks. See KT-22275 for more details.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_10,
        )
    }

    compilerArgument {
        name = "Xno-source-debug-extension"
        description = "Don't generate the '@kotlin.jvm.internal.SourceDebugExtension' annotation with an SMAP copy on classes.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_0,
        )
    }

    compilerArgument {
        name = "Xprofile"
        compilerName = "profileCompilerCommand"
        description = """Debug option: Run the compiler with the async profiler and save snapshots to `outputDir`; `command` is passed to the async profiler on start.
`profilerPath` is the path to libasyncProfiler.so; async-profiler.jar should be on the compiler classpath.
If it's not on the classpath, the compiler will attempt to load async-profiler.jar from the containing directory of profilerPath.
Example: -Xprofile=<PATH_TO_ASYNC_PROFILER>/async-profiler/build/libasyncProfiler.so:event=cpu,interval=1ms,threads,start:<SNAPSHOT_DIR_PATH>""".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<profilerPath:command:outputDir>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_20,
        )
    }

    compilerArgument {
        name = "Xuse-14-inline-classes-mangling-scheme"
        compilerName = "useOldInlineClassesManglingScheme"
        description = "Use the scheme for inline class mangling from version 1.4 instead of the one from 1.4.30.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_30,
        )
    }

    compilerArgument {
        name = "Xjvm-enable-preview"
        compilerName = "enableJvmPreview"
        description = """Allow using Java features that are in the preview phase.
This works like '--enable-preview' in Java. All class files are marked as compiled with preview features, meaning it won't be possible to use them in release environments.""".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_30,
        )
    }

    compilerArgument {
        name = "Xsuppress-deprecated-jvm-target-warning"
        description = """Suppress warnings about deprecated JVM target versions.
This option has no effect and will be deleted in a future version.""".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_0,
        )
    }

    compilerArgument {
        name = "Xtype-enhancement-improvements-strict-mode"
        compilerName = "typeEnhancementImprovementsInStrictMode"
        description = """Enable strict mode for improvements to type enhancement for loaded Java types based on nullability annotations,
including the ability to read type-use annotations from class files.
See KT-45671 for more details.""".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.TypeEnhancementImprovementsInStrictMode))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_0,
        )
    }

    compilerArgument {
        name = "Xserialize-ir"
        description = "Save the IR to metadata (Experimental).".asReleaseDependent()
        valueType = StringType(
            isNullable = false.asReleaseDependent(),
            defaultValue = "none".asReleaseDependent()
        )
        valueDescription = "{none|inline|all}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_6_0,
        )
    }

    compilerArgument {
        name = "Xvalidate-bytecode"
        description = "Validate generated JVM bytecode before and after optimizations.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_6_0,
        )
    }

    compilerArgument {
        name = "Xenhance-type-parameter-types-to-def-not-null"
        description = "Enhance not-null-annotated type parameter types to definitely-non-nullable types ('@NotNull T' => 'T & Any').".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_6_0,
        )
    }

    compilerArgument {
        name = "Xlink-via-signatures"
        description = """Link JVM IR symbols via signatures instead of descriptors.
This mode is slower, but it can be useful for troubleshooting problems with the JVM IR backend.
This option is deprecated and will be deleted in future versions.
It has no effect when -language-version is 2.0 or higher.""".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(
            Deprecated("This flag is deprecated")
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            deprecatedVersion = KotlinReleaseVersion.v2_0_0,
        )
    }

    compilerArgument {
        name = "Xdebug"
        compilerName = "enableDebugMode"
        description = """Enable debug mode for compilation.
Currently this includes spilling all variables in a suspending context regardless of whether they are alive.
If API Level >= 2.2 -- no-op.""".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_0,
        )
    }

    compilerArgument {
        name = "Xenhanced-coroutines-debugging"
        description = """Generate additional linenumber instruction for compiler-generated code
inside suspend functions and lambdas to distinguish them from user code by debugger.""".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
        )
    }

    compilerArgument {
        name = "Xno-new-java-annotation-targets"
        description = "Don't generate Java 1.8+ targets for Kotlin annotation classes.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_0,
        )
    }

    compilerArgument {
        name = "Xvalue-classes"
        description = "Enable experimental value classes.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.ValueClasses))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_20,
        )
    }

    compilerArgument {
        name = "Xir-inliner"
        compilerName = "enableIrInliner"
        description = "Inline functions using the IR inliner instead of the bytecode inliner.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_0,
            removedVersion = KotlinReleaseVersion.v2_3_0
        )
    }

    compilerArgument {
        name = "Xuse-inline-scopes-numbers"
        description = "Use inline scopes numbers for inline marker variables.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_0,
        )
    }

    compilerArgument {
        name = "Xuse-k2-kapt"
        description = "Enable the experimental support for K2 KAPT.".asReleaseDependent()
        valueType = BooleanType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_0,
            removedVersion = KotlinReleaseVersion.v2_3_0,
        )
    }

    compilerArgument {
        name = "Xcompile-builtins-as-part-of-stdlib"
        compilerName = "expectBuiltinsAsPartOfStdlib"
        description = "Enable behaviour needed to compile builtins as part of JVM stdlib".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xoutput-builtins-metadata"
        description = "Output builtins metadata as .kotlin_builtins files".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xannotations-in-metadata"
        description = "Write annotations on declarations into the metadata (in addition to the JVM bytecode), and read annotations from the metadata if they are present.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.AnnotationsInMetadata))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
        )
    }

    compilerArgument {
        name = "Xwhen-expressions"
        compilerName = "whenExpressionsGeneration"
        description = """Select the code generation scheme for type-checking 'when' expressions:
-Xwhen-expressions=indy         Generate type-checking 'when' expressions using 'invokedynamic' with 'SwitchBootstraps.typeSwitch(..)' and 
                                following 'tableswitch' or 'lookupswitch'. This requires '-jvm-target 21' or greater.
-Xwhen-expressions=inline       Generate type-checking 'when' expressions as a chain of type checks.
The default value is 'inline'.""".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "{indy|inline}".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_20
        )
    }
}
