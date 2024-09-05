package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object SpaceiOSK2 : BuildType({
    name = "🍏ᵐ [Project] Space iOS with K2"

    artifactRules = """
                    **/*.hprof=>internal/hprof.zip
                    **/kotlin-daemon*.log=>internal/logs.zip
                    %env.GRADLE_USER_HOME%/**/*daemon*.log=>internal/logs.zip
        **/testKitCache/test-kit-daemon/**/*daemon*.log=>internal/test-kit-daemon-logs.zip
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

    params {
        param("kotlin.artifacts.repository", "file://%teamcity.build.checkoutDir%/artifacts/kotlin/")
        password("env.JB_SPACE_CLIENT_SECRET", "credentialsJSON:631d7d4b-bbab-461d-970f-40f14fb6db1c")
        param("env.GRADLE_USER_HOME", "%teamcity.agent.home.dir%/system/gradle")
        param("env.JB_SPACE_CLIENT_ID", "%kotlin.userprojects.spaceProject.username%")
    }

    vcs {
        root(UserProjectsK2.vcsRoots.SpaceK2VCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVcs, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        script {
            name = "Prepare .netrc"
            scriptContent = """
                #! /usr/bin/env sh
                
                [ "${'$'}DEBUG" == 'true' ] && set -x
                
                NETRC="${'$'}HOME/.netrc"
                SERVER="packages.jetbrains.team"
                
                [ ! -f "${'$'}NETRC" ] && touch "${'$'}NETRC" && chmod 600 "${'$'}NETRC"
                
                if ! grep -q "${'$'}SERVER" "${'$'}NETRC"; then
                    echo "machine ${'$'}SERVER login %env.JB_SPACE_CLIENT_ID% password %env.JB_SPACE_CLIENT_SECRET%" >> "${'$'}NETRC"
                fi
            """.trimIndent()
        }
        script {
            name = "Preparing project files"
            workingDir = "user-project"
            scriptContent = """
                #!/bin/bash
                echo "Space Kotlin version replacement in libs.versions.toml"
                sed -i -e 's#kotlin-lang = \".*\"#kotlin-lang = \"${BuildNumber.depParamRefs["deployVersion"]}\"#g' gradle/libs.versions.toml
                sed -i -e 's#kotlin-plugin = \".*\"#kotlin-plugin = \"${BuildNumber.depParamRefs["deployVersion"]}\"#g' gradle/libs.versions.toml
                echo "replaced Kotlin parameters in libs.versions.toml config"
                cat gradle/libs.versions.toml | grep -E '^kotlin-(lang|plugin) = \".*\"'
                
                echo "Prepare local.properties"
                echo "localRepo=%kotlin.artifacts.repository%" >> local.properties
            """.trimIndent()
        }
        gradle {
            name = "Base tests"
            tasks = ":app:app-ios-native:platformTests :app:app-ios-native:platformTestsAdditional"
            buildFile = "build.gradle"
            workingDir = "user-project"
            gradleParams = """
                --no-configuration-cache
                                        --init-script %teamcity.build.checkoutDir%/community-project-plugin/community-project.init.gradle.kts
                                        --init-script %teamcity.build.checkoutDir%/community-project-plugin/cache-redirector.init.gradle
                                        -Pcommunity.project.kotlin.repo=%kotlin.artifacts.repository%
                                        -Pcommunity.project.kotlin.version=${BuildNumber.depParamRefs["deployVersion"]}
                                        -Pkotlin.native.version=${BuildNumber.depParamRefs["deployVersion"]}
                                        -Pcommunity.project.kotlin.languageVersion=2.1
                                        -Pcommunity.project.kotlin.apiVersion=2.1
                                        -PspaceUsername=%env.JB_SPACE_CLIENT_ID%
                                        -PspacePassword=%env.JB_SPACE_CLIENT_SECRET%
                                        -Pcommunity.project.gradle.repositories.mode=project
                                        -Pcommunity.project.disable.verification.tasks=true
                                        -PincludeIosModules=true
                                        --parallel
                                        --max-workers=2
                                        -Dscan.tag.platform_tests
                                        -Pkotlin.target=IOS
                                        -Pkotlin.build.type=DEBUG
                                        -Dscan.tag.app:app-ios-native --stacktrace --info --continue
            """.trimIndent()
        }
        script {
            name = "Task :app:app-ios-native:buildForTests"
            workingDir = "user-project"
            scriptContent = """
                #!/bin/bash -ex
                
                curl -Lo /tmp/swiftgen.rb https://raw.githubusercontent.com/iMichka/homebrew-core/17ae00b4bf1640cc544eae5f6eec03775c09420b/Formula/swiftgen.rb && brew install /tmp/swiftgen.rb && rm /tmp/swiftgen.rb
                curl -Ls https://install.tuist.io | bash
                
                brew install gpg openssl@3
                curl -sSL https://rvm.io/mpapis.asc | gpg --import -
                curl -sSL https://rvm.io/pkuczynski.asc | gpg --import -
                
                curl -fsSL -o rvm-installer --retry 5 https://raw.githubusercontent.com/rvm/rvm/master/binscripts/rvm-installer
                curl -fsSL -o rvm-installer.asc --retry 5 https://raw.githubusercontent.com/rvm/rvm/master/binscripts/rvm-installer.asc
                
                gpg --verify rvm-installer.asc || exit 10
                
                bash rvm-installer || exit 1
                source ~/.profile
                source ~/.rvm/scripts/rvm
                
                rvm get head
                rvm requirements
                rvm install 3.2.2 --with-openssl-dir=${'$'}(brew --prefix openssl@3)
                rvm cleanup all
                rvm use 3.2.2 --default
                rvm get stable --auto-dotfiles
                
                mkdir ~/.gems
                echo "gem: --user-install --no-document" >> ~/.gemrc
                sudo chown -R admin /Library/Ruby
                gem update --system --force
                gem install bundler:2.3.11 --force
                
                ./gradlew :app:app-ios-native:buildForTests \
                    --stacktrace --info --no-configuration-cache \
                    --init-script %teamcity.build.checkoutDir%/community-project-plugin/community-project.init.gradle.kts \
                    --init-script %teamcity.build.checkoutDir%/community-project-plugin/cache-redirector.init.gradle \
                    -Pcommunity.project.kotlin.repo=%kotlin.artifacts.repository% \
                    -Pcommunity.project.kotlin.version=${BuildNumber.depParamRefs["deployVersion"]} \
                    -Pkotlin.native.version=${BuildNumber.depParamRefs["deployVersion"]} \
                    -Pcommunity.project.kotlin.languageVersion=2.1 \
                    -Pcommunity.project.kotlin.apiVersion=2.1 \
                    -PspaceUsername=%env.JB_SPACE_CLIENT_ID% \
                    -PspacePassword=%env.JB_SPACE_CLIENT_SECRET% \
                    -Pcommunity.project.gradle.repositories.mode=project \
                    -Pcommunity.project.disable.verification.tasks=true \
                    -PincludeIosModules=true \
                    --parallel --max-workers=2 \
                    -Dorg.gradle.daemon=false
            """.trimIndent()
        }
        gradle {
            name = "linkDebugFrameworkIos"
            tasks = ":app:app-ios-native:linkDebugFrameworkIos"
            buildFile = "build.gradle"
            workingDir = "user-project"
            gradleParams = """
                --configuration-cache-problems=warn
                                       --init-script %teamcity.build.checkoutDir%/community-project-plugin/community-project.init.gradle.kts
                                       --init-script %teamcity.build.checkoutDir%/community-project-plugin/cache-redirector.init.gradle
                                       -Pcommunity.project.kotlin.repo=%kotlin.artifacts.repository%
                                       -Pcommunity.project.kotlin.version=${BuildNumber.depParamRefs["deployVersion"]}
                                       -Pkotlin.native.version=${BuildNumber.depParamRefs["deployVersion"]}
                                       -Pcommunity.project.kotlin.languageVersion=2.1
                                       -Pcommunity.project.kotlin.apiVersion=2.1
                                       -Dorg.gradle.daemon=false --stacktrace --info --continue
            """.trimIndent()
        }
    }

    failureConditions {
        executionTimeoutMin = 60
    }

    features {
        swabra {
            filesCleanup = Swabra.FilesCleanup.AFTER_BUILD
            paths = "+:user-project"
        }
        notifications {
            enabled = false
            notifierSettings = slackNotifier {
                connection = "PROJECT_EXT_486"
                sendTo = "kotlin-k2-user-projects-notifications"
                messageFormat = verboseMessageFormat {
                    addStatusText = true
                }
            }
            branchFilter = "+:<default>"
            buildFailed = true
            firstFailureAfterSuccess = true
        }
    }

    dependencies {
        dependency(_Self.buildTypes.Artifacts) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                cleanDestination = true
                artifactRules = "+:maven.zip!**=>artifacts/kotlin"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Mac")
        contains("teamcity.agent.jvm.os.arch", "aarch64")
        contains("system.cloud.profile_id", "aquarius-up-k8s")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})
