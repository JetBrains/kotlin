package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.MavenBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.ant
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object CompilerDistAndMavenArtifactsWithDeployVersionKotlinNativeVersion : BuildType({
    name = "🐧 Compiler Dist and Maven Artifacts (with deploy version Kotlin/Native)"

    artifactRules = """
        **/hs_err*.log=>internal/hs_err.zip
        **/*.hprof=>internal/hprof.zip
        **/build/reports/dependency-verification=>internal/dependency-verification
        build/reports/configuration-cache/**/**/configuration-cache-report.html
        kotlin/build/repo => maven.zip
        **/*-method-count.txt => internal
        kotlin/build/internal/repo => internal/repo
        %teamcity.build.checkoutDir%/kotlin/build/%reproducible.maven.artifact%
    """.trimIndent()
    buildNumberPattern = "%build.number.default%"

    params {
        param("gradleParameters", "%globalGradleParameters%")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("system.deploy-url", "file://%teamcity.build.checkoutDir%/kotlin/build/repo")
        param("DeployVersion", "${BuildNumber.depParamRefs["deployVersion"]}")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("system.deployVersion", "%DeployVersion%", display = ParameterDisplay.HIDDEN, readOnly = true)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("system.versions.kotlin-native", "%DeployVersion%")
        param("build.number.default", "${BuildNumber.depParamRefs.buildNumber}")
        param("mavenParameters", "")
        param("reproducible.maven.artifact", "reproducible-maven-%DeployVersion%.zip")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        gradle {
            name = "Core libraries and compiler dist"
            tasks = "dist dexMethodCount"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Zip compiler with checksum"
            tasks = "zipCompilerChecksum"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        ant {
            name = "Publish Artifacts (compiler)"
            mode = antScript {
                content = """
                    <project name="Publish artifacts" default="publish">
                    <target name="publish">
                    <echo message="##teamcity[publishArtifacts '%teamcity.build.checkoutDir%/kotlin/dist/kotlin-compiler-*']" />
                    </target>
                    </project>
                """.trimIndent()
            }
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            antArguments = "-v"
            jdkHome = "%env.JDK_11_0%"
        }
        maven {
            name = "Set Version"
            goals = "versions:set"
            pomLocation = "%teamcity.build.checkoutDir%/kotlin/libraries/pom.xml"
            runnerArgs = "-DnewVersion=%DeployVersion% -DgenerateBackupPoms=false -DprocessAllModules=true"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            mavenVersion = bundled_3_6()
            localRepoScope = MavenBuildStep.RepositoryScope.BUILD_CONFIGURATION
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Install artifacts to local maven repo"
            tasks = "install publish"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel -Dmaven.repo.local=%teamcity.maven.local.repository.path%"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        maven {
            name = "Maven build"
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            goals = "clean deploy"
            pomLocation = "%teamcity.build.checkoutDir%/kotlin/libraries/pom.xml"
            runnerArgs = "-DskipTests %mavenParameters%"
            workingDir = "%teamcity.build.checkoutDir%/kotlin/libraries"
            mavenVersion = bundled_3_6()
            localRepoScope = MavenBuildStep.RepositoryScope.BUILD_CONFIGURATION
            jdkHome = "%env.JDK_11_0%"
        }
        script {
            name = "Reproducible maven ZIP [%reproducible.maven.artifact%]"
            scriptContent = """
                #!/bin/bash
                set -euo pipefail
                set -x
                
                cp -R "kotlin/build/repo" "%teamcity.build.checkoutDir%/kotlin/build/reproducible_build"
                
                cd "%teamcity.build.checkoutDir%/kotlin/build/reproducible_build"                   
                find . -name "maven-metadata.xml*" -exec rm -rf {} \;
                find . -name "*.asc*" -exec rm -rf {} \;
                find . -exec touch -t "198001010000" {} \;
                find . -name "*.spdx.json*" -exec rm -rf {} \;
                
                find . -type f | sort | zip -X %teamcity.build.checkoutDir%/kotlin/build/%reproducible.maven.artifact% -@
            """.trimIndent()
        }
    }

    failureConditions {
        executionTimeoutMin = 300
    }

    features {
        swabra {
            filesCleanup = Swabra.FilesCleanup.DISABLED
            lockingProcesses = Swabra.LockingProcessPolicy.KILL
        }
        freeDiskSpace {
            requiredSpace = "10gb"
            failBuild = true
        }
        perfmon {
        }
    }

    dependencies {
        snapshot(BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(CompilerDist) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
    }
})
