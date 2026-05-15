/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

        org.jetbrains.kotlin.arguments.description.removed

       org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames
       org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
       org.jetbrains.kotlin.arguments.dsl.base.asReleaseDependent
       org.jetbrains.kotlin.arguments.dsl.base.compilerArgumentsLevel
       org.jetbrains.kotlin.arguments.dsl.defaultFalse
       org.jetbrains.kotlin.arguments.dsl.defaultNull
       org.jetbrains.kotlin.arguments.dsl.types.BooleanType
       org.jetbrains.kotlin.arguments.dsl.types.StringType

    removedCommonCompilerArguments    compilerArgumentsLevel(CompilerArgumentsLevelNames.commonCompilerArguments) 
    compilerArgument 
        name   Xuse-k2
        description 
            "Compile using the experimental K2 compiler pipeline. No compatibility guarantees are provided yet.".asReleaseDependent()
        valueType   BooleanType.defaultFalse

        lifecycle
            introducedVersion   KotlinReleaseVersion.v1_7_0
            deprecatedVersion   KotlinReleaseVersion.v1_9_0
            removedVersion   KotlinReleaseVersion.v2_2_0
        
    

