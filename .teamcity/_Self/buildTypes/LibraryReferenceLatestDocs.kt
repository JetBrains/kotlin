package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object LibraryReferenceLatestDocs : BuildType({
    name = "🐧 Libraries Reference Documentation (Latest)"

    artifactRules = "%system.docsBuildDir%/latest => latest-version.zip"
    buildNumberPattern = "%build.number.default%"

    params {
        param("gradleParameters", "%globalGradleParameters%")
        param("system.deployVersion", "${CompilerDistAndMavenArtifacts.depParamRefs["DeployVersion"]}")
        param("system.githubRevision", "%teamcity.build.branch%")
        param("system.kotlinLibsRepo", "%teamcity.build.checkoutDir%/kotlin/build/repo")
        param("build.number.default", "${BuildNumber.depParamRefs.buildNumber}")
        param("system.docsPreviousVersionsDir", "%teamcity.build.checkoutDir%/kotlin/build/docs-previous")
        param("system.docsBuildDir", "%teamcity.build.checkoutDir%/kotlin/build/docs")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
    }

    steps {
        gradle {
            name = "Docs"
            tasks = "clean buildLatestVersion"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin/libraries/tools/kotlin-stdlib-docs"
            gradleParams = "%gradleParameters% --parallel"
            enableStacktrace = true
            jdkHome = "%env.JDK_11_0%"
        }
    }

    features {
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
        dependency(CompilerDistAndMavenArtifacts) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "maven.zip!org/jetbrains/kotlin/**=>%system.kotlinLibsRepo%/org/jetbrains/kotlin/"
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
