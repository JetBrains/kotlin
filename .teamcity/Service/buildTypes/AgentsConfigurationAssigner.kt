package Service.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object AgentsConfigurationAssigner : BuildType({
    name = "🐧 Agents Configuration Assigner"

    params {
        param("docker.image", "amazoncorretto:21")
        param("env.ASSIGNER_TOKEN", "%teamcity.assigner.token%")
    }

    vcs {
        root(Service.vcsRoots.AgentsConfigurationAssigner_1, "+:. => agents-configuration-assigner")
    }

    steps {
        script {
            name = "assign pool"
            workingDir = "agents-configuration-assigner"
            scriptContent = """./gradlew --no-daemon :run --args="pool kotlin-linux-x64-metal-munit787 Kotlin_BuildPlayground_Aquarius_KotlinNativePerformanceTests""""
            dockerImage = "%docker.image%"
        }
        script {
            name = "assign kotlin-macos-m1-munit620"
            workingDir = "agents-configuration-assigner"
            scriptContent = """./gradlew --no-daemon :run --args="agent kotlin-macos-m1-munit620 Kotlin_BuildPlayground_Aquarius_KotlinNativePerformance_Tests_New_MM_Pre_commit_macos_arm64 Kotlin_BuildPlayground_Aquarius_KotlinNativePerformance_Tests_New_MM_macos_arm64 Kotlin_BuildPlayground_Aquarius_KotlinNativePerformance_Tests_Debug_macos_arm64""""
            dockerImage = "%docker.image%"
        }
        script {
            name = "assign kotlin-linux-x64-metal-munit787"
            workingDir = "agents-configuration-assigner"
            scriptContent = """./gradlew --no-daemon :run --args="agent kotlin-linux-x64-metal-munit787 Kotlin_BuildPlayground_Aquarius_KotlinNativePerformance_Tests_New_MM_Pre_commit_linux_x64 Kotlin_BuildPlayground_Aquarius_KotlinNativePerformance_Tests_New_MM_linux_x64 Kotlin_BuildPlayground_Aquarius_KotlinNativePerformance_Tests_Debug_linux_x64""""
            dockerImage = "%docker.image%"
        }
        script {
            name = "assign kotlin-macos-x64-munit719"
            workingDir = "agents-configuration-assigner"
            scriptContent = """./gradlew --no-daemon :run --args="agent kotlin-macos-x64-munit719 Kotlin_BuildPlayground_Aquarius_KotlinNativePerformance_Tests_New_MM_Pre_commit_macos_x64 Kotlin_BuildPlayground_Aquarius_KotlinNativePerformance_Tests_New_MM_macos_x64 Kotlin_BuildPlayground_Aquarius_KotlinNativePerformance_Tests_Debug_macos_x64""""
            dockerImage = "%docker.image%"
        }
        script {
            name = "assign kotlin-windows-x64-munit667"
            workingDir = "agents-configuration-assigner"
            scriptContent = """./gradlew --no-daemon :run --args="agent kotlin-windows-x64-munit667 Kotlin_BuildPlayground_Aquarius_KotlinNativePerformance_Tests_New_MM_Pre_commit_mingw_x64 Kotlin_BuildPlayground_Aquarius_KotlinNativePerformance_Tests_New_MM_mingw_x64 Kotlin_BuildPlayground_Aquarius_KotlinNativePerformance_Tests_Debug_mingw_x64""""
            dockerImage = "%docker.image%"
        }
    }

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_774"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})
