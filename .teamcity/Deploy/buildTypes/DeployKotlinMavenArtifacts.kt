package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.MavenBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object DeployKotlinMavenArtifacts : BuildType({
    name = "🐧 Deploy Kotlin Maven Artifacts"

    artifactRules = """
        kotlin/build/repo=>maven-%DeployVersion%.zip
        %teamcity.build.checkoutDir%/kotlin/build/%reproducible.maven.artifact%
        **/hs_err*.log=>internal/hs_err.zip
        **/*.hprof=>internal/hprof.zip
        **/build/reports/dependency-verification=>internal/dependency-verification
        build/reports/configuration-cache/**/**/configuration-cache-report.html
        kotlin/build/local-publish=>local-publish-maven-%DeployVersion%.zip
    """.trimIndent()
    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("gradleParameters", "%globalGradleParameters%")
        text("DeployVersion", "${BuildNumber.depParamRefs["deployVersion"]}", display = ParameterDisplay.PROMPT)
        text("deploy-repo", "local", display = ParameterDisplay.PROMPT)
        password("system.kotlin.kotlin-space-packages.user", "credentialsJSON:70dd5a56-00b7-43a9-a2b7-e9c802b5d17d", display = ParameterDisplay.HIDDEN)
        param("mavenParameters", "")
        param("reproducible.maven.artifact", "reproducible-maven-Kotlin_BuildPlayground_Aquarius_DeployKotlinMavenArtifacts-%DeployVersion%.zip")
        password("system.kotlin.kotlin-space-packages.password", "credentialsJSON:6a448008-2992-4f63-9a64-2e5013887a2e", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("versions.kotlin-native", "%DeployVersion%")
        text("deploy-url", "file://%teamcity.build.checkoutDir%/kotlin/build/local-publish", display = ParameterDisplay.PROMPT)
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        script {
            name = "Prepare gnupg"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            scriptContent = """
                cd libraries
                export HOME=${'$'}(pwd)
                export GPG_TTY=${'$'}(tty)
                
                rm -rf .gnupg
                
                cat >keyfile <<EOT
                %sign.key.private.new%
                EOT
                gpg --allow-secret-key-import --batch --import keyfile
                rm -v keyfile
                
                cat >keyfile <<EOT
                %sign.key.main.public%
                EOT
                gpg --batch --import keyfile
                rm -v keyfile
            """.trimIndent()
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
            name = "Gradle install and publish"
            tasks = "install publish"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --no-parallel -PdeployVersion=%DeployVersion% -Pversions.kotlin-native=%versions.kotlin-native% -Dmaven.repo.local=%teamcity.maven.local.repository.path% -PsigningRequired=true -Psigning.gnupg.executable=gpg -Psigning.gnupg.keyName=%sign.key.id.new% -Psigning.gnupg.homeDir=%teamcity.build.checkoutDir%/kotlin/libraries/.gnupg -Psigning.gnupg.passphrase=%sign.key.passphrase.new% --no-scan --info --stacktrace --no-build-cache -Pdeploy-repo=%deploy-repo% -Pdeploy-url=%deploy-url%"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        maven {
            name = "Maven publish"
            goals = "clean deploy"
            pomLocation = "%teamcity.build.checkoutDir%/kotlin/libraries/pom.xml"
            runnerArgs = "-Dinvoker.skip=true -DskipTests -Dkotlin.key.name=%sign.key.id.new% -Dkotlin.key.passphrase=%sign.key.passphrase.new% --activate-profiles noTest,sign-artifacts -e %mavenParameters% -Ddeploy-repo=%deploy-repo% -Ddeploy-url=%deploy-url%"
            workingDir = "%teamcity.build.checkoutDir%/kotlin/libraries"
            mavenVersion = bundled_3_6()
            userSettingsSelection = "userSettingsSelection:byPath"
            userSettingsPath = "%teamcity.build.checkoutDir%/kotlin/libraries/maven-settings.xml"
            localRepoScope = MavenBuildStep.RepositoryScope.BUILD_CONFIGURATION
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Gradle publish to local dir repo"
            tasks = "publish"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --no-parallel -PdeployVersion=%DeployVersion% -Pversions.kotlin-native=%versions.kotlin-native% -Dmaven.repo.local=%teamcity.maven.local.repository.path% -PsigningRequired=true -Psigning.gnupg.executable=gpg -Psigning.gnupg.keyName=%sign.key.id.new% -Psigning.gnupg.homeDir=%teamcity.build.checkoutDir%/kotlin/libraries/.gnupg -Psigning.gnupg.passphrase=%sign.key.passphrase.new% --no-scan --info --stacktrace --no-build-cache -Pdeploy-repo=local -Pdeploy-url=file://%teamcity.build.checkoutDir%/kotlin/build/repo"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        maven {
            name = "Maven publish to local dir repo"
            goals = "clean deploy"
            pomLocation = "%teamcity.build.checkoutDir%/kotlin/libraries/pom.xml"
            runnerArgs = "-Dinvoker.skip=true -DskipTests -Dkotlin.key.name=%sign.key.id.new% -Dkotlin.key.passphrase=%sign.key.passphrase.new% --activate-profiles noTest,sign-artifacts -e %mavenParameters% -Ddeploy-repo=local -Ddeploy-url=file://%teamcity.build.checkoutDir%/kotlin/build/repo"
            workingDir = "%teamcity.build.checkoutDir%/kotlin/libraries"
            mavenVersion = bundled_3_6()
            userSettingsSelection = "userSettingsSelection:byPath"
            userSettingsPath = "%teamcity.build.checkoutDir%/kotlin/libraries/maven-settings.xml"
            localRepoScope = MavenBuildStep.RepositoryScope.BUILD_CONFIGURATION
            jdkHome = "%env.JDK_11_0%"
        }
        script {
            name = "Reproducible maven ZIP [%reproducible.maven.artifact%]"
            scriptContent = """
                #!/bin/bash
                set -euo pipefail
                set -x
                
                cp -R "%teamcity.build.checkoutDir%/kotlin/build/repo" "%teamcity.build.checkoutDir%/kotlin/build/reproducible_repo"
                
                cd "%teamcity.build.checkoutDir%/kotlin/build/reproducible_repo"                   
                find . -name "maven-metadata.xml*" -exec rm -rf {} \;
                find . -name "*.asc*" -exec rm -rf {} \;
                find . -exec touch -t "198001010000" {} \;
                find . -name "*.spdx.json*" -exec rm -rf {} \;
                
                find . -type f | sort | zip -X %teamcity.build.checkoutDir%/kotlin/build/%reproducible.maven.artifact% -@
            """.trimIndent()
        }
        script {
            name = "Cleanup"
            executionMode = BuildStep.ExecutionMode.ALWAYS
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            scriptContent = """
                cd libraries
                rm -rf .gnupg
            """.trimIndent()
        }
        script {
            name = "Add information about repository to the status"
            executionMode = BuildStep.ExecutionMode.ALWAYS
            scriptContent = """
                #!/bin/bash
                set -e
                set -x
                
                echo "##teamcity[buildStatus text='%deploy-url% {build.status.text}']"
            """.trimIndent()
        }
    }

    failureConditions {
        executionTimeoutMin = 120
        errorMessage = true
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
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        contains("system.cloud.profile_id", "-deployment")
        startsWith("system.cloud.profile_id", "aquarius")
    }
})
