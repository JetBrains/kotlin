package Tests_Linux.buildTypes

import _Self.buildTypes.BuildNumber
import _Self.buildTypes.CompilerDistAndMavenArtifacts
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object BootstrapTestFir_LINUX : BuildType({
    name = "🐧 Test Compiler Bootstrapping K2"

    artifactRules = """
        **/hs_err*.log=>internal/hs_err.zip
        **/*.hprof=>internal/hprof.zip
        **/build/reports/dependency-verification=>internal/dependency-verification
        build/reports/configuration-cache/**/**/configuration-cache-report.html
        **/build/reports/tests/**=>internal/test_results.zip
        **/build/test-results/**=>internal/test_results.zip
        **/build.log=>internal/test_results.zip
        build/internal/repo => internal/repo
        native-memory-tracking.txt=>internal
        heap-dump-on-low-memory.txt=>internal
    """.trimIndent()
    buildNumberPattern = "%build.number.default%"

    params {
        param("gradleParameters", "%globalGradleParameters% -Dkotlin.daemon.jvm.options=-Xmx4096m -Pkotlin.build.useFir=true -Pkotlin.build.test.process.NativeMemoryTracking=detail -PdeployVersion=%DeployVersion% -Pbuild.number=%build.number% -Pbootstrap.local=true -Pbootstrap.local.version=${CompilerDistAndMavenArtifacts.depParamRefs["DeployVersion"]} -Pbootstrap.local.path=%teamcity.build.checkoutDir%/bootstrap")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("DeployVersion", "${BuildNumber.depParamRefs["deployVersion"]}")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("bootstrapTestTasks", "classes testClasses testsForBootstrapBuildTest :kotlin-stdlib:test")
        param("build.number.default", "${CompilerDistAndMavenArtifacts.depParamRefs.buildNumber}-bootstrap")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        script {
            name = "Setup heap-dump-on-low-memory.sh background task"
            scriptContent = """
                #!/bin/bash
                tee "heap-dump-on-low-memory.sh" <<'EOF'
                #!/bin/bash
                while true
                do
                    if [ ${'$'}(awk '${'$'}1=="MemTotal:"{t=${'$'}2} ${'$'}1=="MemAvailable:"{a=${'$'}2} END{printf "%d", (a*100)/t}' /proc/meminfo) -le 5 ]; then
                        echo "Low available memory, dumping the largest process..." >> heap-dump-on-low-memory.txt
                        ps -eo rss,vsize,pmem,pid,cmd | sort -k 1 -nr | head -1 >> heap-dump-on-low-memory.txt
                        
                        PID=${'$'}(ps -eo rss,pid | sort -k 1 -nr | head -1 | awk '{print ${'$'}2}')
                        
                        echo "/proc/${'$'}PID/maps" >> heap-dump-on-low-memory.txt
                        cat /proc/${'$'}PID/maps >> heap-dump-on-low-memory.txt
                        
                        jcmd ${'$'}PID VM.native_memory detail >> native-memory-tracking.txt
                        
                        jmap -dump:format=b,file=heap-dump-on-low-memory.hprof ${'$'}PID
                        EXITCODE=${'$'}?
                        test ${'$'}EXITCODE -eq 0 && echo "Memory dump successfully created" >> heap-dump-on-low-memory.txt || echo "Failed to dump memory" >> heap-dump-on-low-memory.txt
                        exit ${'$'}EXITCODE
                    fi
                    sleep 1
                done
                EOF
                chmod +x "heap-dump-on-low-memory.sh"
                ./heap-dump-on-low-memory.sh &
            """.trimIndent()
        }
        gradle {
            name = "Compile All Kotlin"
            tasks = "compileKotlin"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Compile Tests in Kotlin"
            tasks = "compileTestKotlin"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Compile everything before running tests"
            tasks = "%bootstrapTestTasks%"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel --continue -Pkotlin.build.disable.verification.tasks=true -Dscan.tag.verification-tasks-disabled"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Bootstrap tests"
            tasks = "%bootstrapTestTasks%"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel --continue"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        script {
            name = "Cleanup heap-dump-on-low-memory.sh background task"
            executionMode = BuildStep.ExecutionMode.ALWAYS
            scriptContent = """
                #!/bin/bash
                                    pkill -f heap-dump-on-low-memory.sh
                                    rm heap-dump-on-low-memory.sh
            """.trimIndent()
        }
    }

    failureConditions {
        executionTimeoutMin = 180
    }

    features {
        swabra {
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
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        dependency(_Self.buildTypes.CompilerDistAndMavenArtifacts) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "maven.zip!** => bootstrap"
            }
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
