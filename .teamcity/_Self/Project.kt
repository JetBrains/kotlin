package _Self

import _Self.buildTypes.*
import _Self.vcsRoots.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.CustomChart
import jetbrains.buildServer.configs.kotlin.CustomChart.*
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.amazonEC2CloudImage
import jetbrains.buildServer.configs.kotlin.amazonEC2CloudProfile
import jetbrains.buildServer.configs.kotlin.buildTypeCustomChart
import jetbrains.buildServer.configs.kotlin.kubernetesCloudImage
import jetbrains.buildServer.configs.kotlin.kubernetesCloudProfile
import jetbrains.buildServer.configs.kotlin.projectCustomChart

object Project : Project({
    description = "Kotlin Compiler, IDEA plugin and tests: mainline development branches"

    vcsRoot(CommunityProjectPluginVcs)
    vcsRoot(KotlinxCoroutinesK2VCS)
    vcsRoot(SerializationVCS)
    vcsRoot(TeamCityBuild)
    vcsRoot(IntellijMonorepoForKotlinVCS_kt_master)
    vcsRoot(ktorioktor)
    vcsRoot(ExposedVCS)
    vcsRoot(KotlinTeamcityBuild)
    vcsRoot(benchmarkForAggregateVCS)

    buildType(BuildGradleIntegrationTests)
    buildType(SafeMergeCoordinator)
    buildType(LibraryReferenceLegacyDocs)
    buildType(LibraryReferenceLatestDocs)
    buildType(Aggregate)
    buildType(KotlinNativeSanityRemoteRunComposite)
    buildType(Quarantine)
    buildType(CompileAllClasses)
    buildType(Nightly)
    buildType(CompilerDist)
    buildType(BuildCacheTests)
    buildType(AggregateWithNativePreCommit)
    buildType(CompilerDistAndMavenArtifactsForIde)
    buildType(SafeMerge)
    buildType(CompilerArtifacts)
    buildType(KotlinxLibrariesCompilation)
    buildType(MavenArtifactsAgent)
    buildType(BuildKotlinToDirectoryCache)
    buildType(CompilerArtifactsInDocker)
    buildType(PublishToNpmDryRun)
    buildType(CompilerDistLocal)
    buildType(CompilerDistAndMavenArtifactsWithDeployVersionKotlinNativeVersion)
    buildType(MavenArtifactsDocker)
    buildType(CompilerDistLocalK2)
    buildType(SafeMergeAggregate)
    buildType(Artifacts)
    buildType(NightlyCritical)
    buildType(ValidateIdePluginDependencies)
    buildType(TriggerUserProjectsTcc)
    buildType(BuildNumber)
    buildType(ResolveDependencies)
    buildType(CompilerDistAndMavenArtifacts)
    buildType(CompilerDistLocalOverrideObsoleteJdk)

    params {
        param("build.number.native.meta.version", "aquarius")
        param("teamcity.internal.gradle.runner.launch.mode", "gradle-tooling-api")
        param("teamcity.internal.git.sshDebug", "true")
        param("teamcity.internal.webhooks.events", "BUILD_FINISHED")
        param("globalGradleParameters", """-Pteamcity=true "-Pbuild.number=%build.number%" --configuration-cache %globalGradleCacheNodeParameters% %globalGradleBuildScanParameters%""")
        param("teamcity.internal.webhooks.url", "https://3z66zlayx9.execute-api.eu-west-1.amazonaws.com/1")
        param("build.number.prefix", "2.1.0-aquarius")
        param("teamcity.internal.webhooks.enable", "false")
        param("globalGradleCacheNodeParameters", " -Pkotlin.build.cache.url=https://gradle-cache.kotlin.intellij.net/cache/ -Pkotlin.build.cache.user=%kotlin.build.cache.user% -Pkotlin.build.cache.password=%kotlin.build.cache.password% -Pkotlin.build.cache.push=true")
        text("teamcity.activeVcsBranch.age.days", "5", display = ParameterDisplay.HIDDEN)
        param("globalGradleBuildScanParameters", "-Pkotlin.build.scan.url=%gradle.enterprise.url% -Dscan.tag.kotlin-aquarius")
    }

    features {
        projectCustomChart {
            id = "KotlinCompilerQueueWaitReasonTime"
            title = "Kotlin Compiler Queue Wait Reason Time"
            seriesTitle = "Reason"
            format = CustomChart.Format.DURATION
            series = listOf(
                Serie(title = "Total: Time Spent in Queue", key = SeriesKey.QUEUE_TIME, sourceBuildTypeId = "Kotlin_BuildPlayground_Aquarius_CompilerDistAndMavenArtifacts"),
                Serie(title = "Agent: All compatible agents are outdated - waiting for upgrade", key = SeriesKey("queueWaitReason:All_compatible_agents_are_outdated__waiting_for_upgrade"), sourceBuildTypeId = "Kotlin_BuildPlayground_Aquarius_CompilerDistAndMavenArtifacts"),
                Serie(title = "Agent: Waiting for starting agent", key = SeriesKey("queueWaitReason:Waiting_for_starting_agent"), sourceBuildTypeId = "Kotlin_BuildPlayground_Aquarius_CompilerDistAndMavenArtifacts"),
                Serie(title = "Agent: There are no compatible agents which can run this build", key = SeriesKey("queueWaitReason:There_are_no_compatible_agents_which_can_run_this_build"), sourceBuildTypeId = "Kotlin_BuildPlayground_Aquarius_CompilerDistAndMavenArtifacts"),
                Serie(title = "Dependencies: Build dependencies have not been built yet", key = SeriesKey("queueWaitReason:Build_dependencies_have_not_been_built_yet"), sourceBuildTypeId = "Kotlin_BuildPlayground_Aquarius_CompilerDistAndMavenArtifacts")
            )
        }
        projectCustomChart {
            id = "KotlinMethodCount"
            title = "Method count"
            seriesTitle = "Serie"
            format = CustomChart.Format.INTEGER
            series = listOf(
                Serie(title = "kotlin-stdlib", key = SeriesKey("DexMethodCount_kotlin-stdlib"), sourceBuildTypeId = "Kotlin_BuildPlayground_Aquarius_CompilerDistAndMavenArtifacts"),
                Serie(title = "kotlin-reflect", key = SeriesKey("DexMethodCount_kotlin-reflect"), sourceBuildTypeId = "Kotlin_BuildPlayground_Aquarius_CompilerDistAndMavenArtifacts")
            )
        }
        kubernetesCloudImage {
            id = "PROJECT_EXT_1"
            profileId = "aquarius-kotlin-k8s"
            agentPoolId = "-2"
            agentNamePrefix = "aquarius-kotlin-k8s-latest-xcode-kmp-test-macos"
            maxInstancesCount = 2
            podSpecification = customTemplate {
                customPod = """
                    ---
                    apiVersion: v1
                    kind: Pod
                    metadata:
                      # name: would be set as %agent-name-prefix%-%id%
                      # namespace: would be set from cloud-profile
                      labels:
                        agent-id: %instance.id%
                    spec:
                      enableServiceLinks: false
                      securityContext:
                        runAsUser: 20 # =admin
                      containers:
                        - name: macos
                          image: mac-registry.eqx.k8s.intellij.net/kotlin/kmm-macos-arm64-agent-staging:latest
                          imagePullPolicy: Always
                          resources:
                            requests:
                              cpu: 7
                              memory: 16Gi
                            limits:
                              cpu: 7
                              memory: 16Gi
                          command:
                          - "bash"
                          - "-c"
                          - |
                            /Users/admin/agent-launcher.sh
                """.trimIndent()
            }
        }
        buildTypeCustomChart {
            id = "PROJECT_EXT_165"
            title = "Import duration"
            seriesTitle = "Serie"
            format = CustomChart.Format.TEXT
            series = listOf(
                Serie(title = "import_duration", key = SeriesKey("import_duration"))
            )
        }
        kubernetesCloudImage {
            id = "PROJECT_EXT_2"
            profileId = "aquarius-kotlin-k8s"
            agentPoolId = "-2"
            agentNamePrefix = "aquarius-kotlin-k8s-native-macos-x64"
            maxInstancesCount = 1
            podSpecification = customTemplate {
                customPod = """
                    ---
                    apiVersion: v1
                    kind: Pod
                    metadata:
                      # name: would be set as %agent-name-prefix%-%id%
                      # namespace: would be set from cloud-profile
                      labels:
                        agent-id: %instance.id%
                    spec:
                      enableServiceLinks: false
                      securityContext:
                        runAsUser: 20 # =admin
                      containers:
                        - name: macos
                          image: mac-registry.eqx.k8s.intellij.net/kotlin/kotlin-native-macos-x64-agent:xcode-15.3
                          imagePullPolicy: Always
                          resources:
                            requests:
                              cpu: 7
                              memory: 16Gi
                            limits:
                              cpu: 7
                              memory: 16Gi
                          command:
                          - "bash"
                          - "-c"
                          - |
                            arch -x86_64 /Users/admin/agent-launcher.sh
                """.trimIndent()
            }
        }
        kubernetesCloudImage {
            id = "PROJECT_EXT_3"
            profileId = "aquarius-kotlin-k8s"
            agentPoolId = "-2"
            agentNamePrefix = "aquarius-kotlin-k8s-native-macos-arm64"
            maxInstancesCount = 1
            podSpecification = customTemplate {
                customPod = """
                    ---
                    apiVersion: v1
                    kind: Pod
                    metadata:
                      # name: would be set as %agent-name-prefix%-%id%
                      # namespace: would be set from cloud-profile
                      labels:
                        agent-id: %instance.id%
                    spec:
                      enableServiceLinks: false
                      securityContext:
                        runAsUser: 20 # =admin
                      containers:
                        - name: macos
                          image: mac-registry.eqx.k8s.intellij.net/kotlin/kotlin-native-macos-arm64-agent:xcode-15.3
                          imagePullPolicy: Always
                          resources:
                            requests:
                              cpu: 7
                              memory: 16Gi
                            limits:
                              cpu: 7
                              memory: 16Gi
                          command:
                          - "bash"
                          - "-c"
                          - |
                            /Users/admin/agent-launcher.sh
                """.trimIndent()
            }
        }
        kubernetesCloudImage {
            id = "PROJECT_EXT_4"
            profileId = "aquarius-kotlin-k8s"
            agentPoolId = "-2"
            agentNamePrefix = "aquarius-kotlin-k8s-native-xcode-stable-macos"
            maxInstancesCount = 1
            podSpecification = customTemplate {
                customPod = """
                    ---
                    apiVersion: v1
                    kind: Pod
                    metadata:
                      # name: would be set as %agent-name-prefix%-%id%
                      # namespace: would be set from cloud-profile
                      labels:
                        agent-id: %instance.id%
                    spec:
                      enableServiceLinks: false
                      securityContext:
                        runAsUser: 20 # =admin
                      containers:
                        - name: macos
                          image: mac-registry.eqx.k8s.intellij.net/kotlin/kotlin-native-arm64-xcode-stable-agent:latest
                          imagePullPolicy: Always
                          resources:
                            requests:
                              cpu: 7
                              memory: 16Gi
                            limits:
                              cpu: 7
                              memory: 16Gi
                          command:
                          - "bash"
                          - "-c"
                          - |
                            /Users/admin/agent-launcher.sh
                """.trimIndent()
            }
        }
        kubernetesCloudImage {
            id = "PROJECT_EXT_5"
            profileId = "aquarius-kotlin-k8s"
            agentPoolId = "-2"
            agentNamePrefix = "aquarius-kotlin-k8s-native-xcode-beta-macos"
            maxInstancesCount = 1
            podSpecification = customTemplate {
                customPod = """
                    ---
                    apiVersion: v1
                    kind: Pod
                    metadata:
                      # name: would be set as %agent-name-prefix%-%id%
                      # namespace: would be set from cloud-profile
                      labels:
                        agent-id: %instance.id%
                    spec:
                      enableServiceLinks: false
                      securityContext:
                        runAsUser: 20 # =admin
                      containers:
                        - name: macos
                          image: mac-registry.eqx.k8s.intellij.net/kotlin/kotlin-native-arm64-xcode-beta-agent:latest
                          imagePullPolicy: Always
                          resources:
                            requests:
                              cpu: 7
                              memory: 16Gi
                            limits:
                              cpu: 7
                              memory: 16Gi
                          command:
                          - "bash"
                          - "-c"
                          - |
                            /Users/admin/agent-launcher.sh
                """.trimIndent()
            }
        }
        amazonEC2CloudProfile {
            id = "aquarius-aws"
            name = "aquarius-aws"
            terminateIdleMinutes = 30
            region = AmazonEC2CloudProfile.Regions.EU_WEST_DUBLIN
            authType = accessKey {
                keyId = "credentialsJSON:6176dae8-b17c-41a3-bb98-2fbd865083bc"
                secretKey = "credentialsJSON:1be3bac3-69f9-49ee-9465-a46431abbdd1"
            }
        }
        amazonEC2CloudProfile {
            id = "aquarius-aws-on-demand"
            name = "aquarius-aws-on-demand"
            terminateIdleMinutes = 30
            region = AmazonEC2CloudProfile.Regions.EU_WEST_DUBLIN
            authType = accessKey {
                keyId = "credentialsJSON:6176dae8-b17c-41a3-bb98-2fbd865083bc"
                secretKey = "credentialsJSON:1be3bac3-69f9-49ee-9465-a46431abbdd1"
            }
        }
        amazonEC2CloudProfile {
            id = "aquarius-aws-on-demand-linux"
            name = "aquarius-aws-on-demand-linux"
            terminateIdleMinutes = 30
            region = AmazonEC2CloudProfile.Regions.EU_WEST_DUBLIN
            authType = accessKey {
                keyId = "credentialsJSON:6176dae8-b17c-41a3-bb98-2fbd865083bc"
                secretKey = "credentialsJSON:1be3bac3-69f9-49ee-9465-a46431abbdd1"
            }
        }
        amazonEC2CloudProfile {
            id = "aquarius-deployment"
            name = "aquarius-deployment"
            terminateIdleMinutes = 30
            region = AmazonEC2CloudProfile.Regions.EU_WEST_DUBLIN
            authType = accessKey {
                keyId = "credentialsJSON:6176dae8-b17c-41a3-bb98-2fbd865083bc"
                secretKey = "credentialsJSON:1be3bac3-69f9-49ee-9465-a46431abbdd1"
            }
        }
        amazonEC2CloudImage {
            id = "aquarius-deployment-linux-4cpu-16gb-a"
            profileId = "aquarius-deployment"
            agentPoolId = "-2"
            name = "aquarius-deployment-linux-4cpu-16gb-a"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "m5d.xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-0e33db0206e40d718"
            source = LaunchTemplate(templateId = "lt-0c4b119ddc5c4c0cd", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-deployment-linux-4cpu-16gb-b"
            profileId = "aquarius-deployment"
            agentPoolId = "-2"
            name = "aquarius-deployment-linux-4cpu-16gb-b"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "m5d.xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-0e33db0206e40d718"
            source = LaunchTemplate(templateId = "lt-05363fe22d228d055", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-deployment-linux-4cpu-16gb-c"
            profileId = "aquarius-deployment"
            agentPoolId = "-2"
            name = "aquarius-deployment-linux-4cpu-16gb-c"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "m5d.xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-0e33db0206e40d718"
            source = LaunchTemplate(templateId = "lt-0247190adbe8bf152", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-deployment-linux-8cpu-16gb-a"
            profileId = "aquarius-deployment"
            agentPoolId = "-2"
            name = "aquarius-deployment-linux-8cpu-16gb-a"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "c5d.2xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-0e33db0206e40d718"
            source = LaunchTemplate(templateId = "lt-0c4b119ddc5c4c0cd", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-deployment-linux-8cpu-16gb-b"
            profileId = "aquarius-deployment"
            agentPoolId = "-2"
            name = "aquarius-deployment-linux-8cpu-16gb-b"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "c5d.2xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-0e33db0206e40d718"
            source = LaunchTemplate(templateId = "lt-05363fe22d228d055", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-deployment-linux-8cpu-16gb-c"
            profileId = "aquarius-deployment"
            agentPoolId = "-2"
            name = "aquarius-deployment-linux-8cpu-16gb-c"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "c5d.2xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-0e33db0206e40d718"
            source = LaunchTemplate(templateId = "lt-0247190adbe8bf152", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        kubernetesCloudProfile {
            id = "aquarius-kotlin-k8s"
            name = "aquarius-kotlin-k8s"
            terminateAfterBuild = true
            terminateIdleMinutes = 30
            apiServerURL = "https://eqx.k8s.intellij.net:6443"
            namespace = "agents-kotlin"
            authStrategy = token {
                token = "credentialsJSON:3f7bc0d8-6183-4618-8587-8956091bd49c"
            }
        }
        amazonEC2CloudImage {
            id = "aquarius-on-demand-linux-4cpu-16gb-a"
            profileId = "aquarius-aws-on-demand-linux"
            agentPoolId = "-2"
            name = "aquarius-on-demand-linux-4cpu-16gb-a"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "m5d.xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 400
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-0e33db0206e40d718"
            source = LaunchTemplate(templateId = "lt-0c4b119ddc5c4c0cd", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-on-demand-linux-4cpu-16gb-b"
            profileId = "aquarius-aws-on-demand-linux"
            agentPoolId = "-2"
            name = "aquarius-on-demand-linux-4cpu-16gb-b"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "m5d.xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 400
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-0e33db0206e40d718"
            source = LaunchTemplate(templateId = "lt-05363fe22d228d055", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-on-demand-linux-4cpu-16gb-c"
            profileId = "aquarius-aws-on-demand-linux"
            agentPoolId = "-2"
            name = "aquarius-on-demand-linux-4cpu-16gb-c"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "m5d.xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 400
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-0e33db0206e40d718"
            source = LaunchTemplate(templateId = "lt-0247190adbe8bf152", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-on-demand-linux-8cpu-16gb-a"
            profileId = "aquarius-aws-on-demand-linux"
            agentPoolId = "-2"
            name = "aquarius-on-demand-linux-8cpu-16gb-a"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "c5d.2xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 400
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-0e33db0206e40d718"
            source = LaunchTemplate(templateId = "lt-0c4b119ddc5c4c0cd", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-on-demand-linux-8cpu-16gb-b"
            profileId = "aquarius-aws-on-demand-linux"
            agentPoolId = "-2"
            name = "aquarius-on-demand-linux-8cpu-16gb-b"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "c5d.2xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 400
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-0e33db0206e40d718"
            source = LaunchTemplate(templateId = "lt-05363fe22d228d055", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-on-demand-linux-8cpu-16gb-c"
            profileId = "aquarius-aws-on-demand-linux"
            agentPoolId = "-2"
            name = "aquarius-on-demand-linux-8cpu-16gb-c"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "c5d.2xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 400
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-0e33db0206e40d718"
            source = LaunchTemplate(templateId = "lt-0247190adbe8bf152", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-on-demand-linux-8cpu-32gb-a"
            profileId = "aquarius-aws-on-demand-linux"
            agentPoolId = "-2"
            name = "aquarius-on-demand-linux-8cpu-32gb-a"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "m5d.2xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 200
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-0e33db0206e40d718"
            source = LaunchTemplate(templateId = "lt-0c4b119ddc5c4c0cd", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-on-demand-linux-8cpu-32gb-b"
            profileId = "aquarius-aws-on-demand-linux"
            agentPoolId = "-2"
            name = "aquarius-on-demand-linux-8cpu-32gb-b"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "m5d.2xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 200
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-0e33db0206e40d718"
            source = LaunchTemplate(templateId = "lt-05363fe22d228d055", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-on-demand-linux-8cpu-32gb-c"
            profileId = "aquarius-aws-on-demand-linux"
            agentPoolId = "-2"
            name = "aquarius-on-demand-linux-8cpu-32gb-c"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "m5d.2xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 200
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-0e33db0206e40d718"
            source = LaunchTemplate(templateId = "lt-0247190adbe8bf152", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-on-demand-windows-4cpu-16gb-a"
            profileId = "aquarius-aws-on-demand"
            agentPoolId = "-2"
            name = "aquarius-on-demand-windows-4cpu-16gb-a"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "m5d.xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-02166855a08d5d8f0"
            source = LaunchTemplate(templateId = "lt-0c4b119ddc5c4c0cd", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-on-demand-windows-4cpu-16gb-b"
            profileId = "aquarius-aws-on-demand"
            agentPoolId = "-2"
            name = "aquarius-on-demand-windows-4cpu-16gb-b"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "m5d.xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-02166855a08d5d8f0"
            source = LaunchTemplate(templateId = "lt-05363fe22d228d055", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-on-demand-windows-4cpu-16gb-c"
            profileId = "aquarius-aws-on-demand"
            agentPoolId = "-2"
            name = "aquarius-on-demand-windows-4cpu-16gb-c"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "m5d.xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-02166855a08d5d8f0"
            source = LaunchTemplate(templateId = "lt-0247190adbe8bf152", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-on-demand-windows-8cpu-16gb-a"
            profileId = "aquarius-aws-on-demand"
            agentPoolId = "-2"
            name = "aquarius-on-demand-windows-8cpu-16gb-a"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "c5d.2xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-02166855a08d5d8f0"
            source = LaunchTemplate(templateId = "lt-0c4b119ddc5c4c0cd", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-on-demand-windows-8cpu-16gb-b"
            profileId = "aquarius-aws-on-demand"
            agentPoolId = "-2"
            name = "aquarius-on-demand-windows-8cpu-16gb-b"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "c5d.2xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-02166855a08d5d8f0"
            source = LaunchTemplate(templateId = "lt-05363fe22d228d055", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "aquarius-on-demand-windows-8cpu-16gb-c"
            profileId = "aquarius-aws-on-demand"
            agentPoolId = "-2"
            name = "aquarius-on-demand-windows-8cpu-16gb-c"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "c5d.2xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-02166855a08d5d8f0"
            source = LaunchTemplate(templateId = "lt-0247190adbe8bf152", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
    }

    cleanup {
        keepRule {
            id = "Keep Logs and Statistics for 92 days"
            keepAtLeast = days(92)
            applyToBuilds {
                inBranches {
                    branchFilter = patterns("+:<default>")
                }
            }
            dataToKeep = historyAndStatistics {
                preserveLogs = true
            }
        }
        baseRule {
            artifacts(builds = 1, days = 7)
        }
    }
    buildTypesOrder = arrayListOf(BuildNumber, Aggregate, Quarantine, SafeMerge, SafeMergeAggregate, Nightly, NightlyCritical, AggregateWithNativePreCommit, BuildCacheTests, KotlinNativeSanityRemoteRunComposite, KotlinxLibrariesCompilation, SafeMergeCoordinator, BuildKotlinToDirectoryCache, CompilerDistAndMavenArtifacts, CompilerDistAndMavenArtifactsWithDeployVersionKotlinNativeVersion, CompilerArtifacts, CompilerArtifactsInDocker, MavenArtifactsDocker, MavenArtifactsAgent, TriggerUserProjectsTcc, PublishToNpmDryRun, CompilerDistAndMavenArtifactsForIde, BuildGradleIntegrationTests, LibraryReferenceLegacyDocs, LibraryReferenceLatestDocs, Artifacts, ResolveDependencies, CompilerDist, CompilerDistLocal, CompilerDistLocalOverrideObsoleteJdk, CompilerDistLocalK2, CompileAllClasses, ValidateIdePluginDependencies)

    subProject(Deploy.Project)
    subProject(KotlinNative.Project)
    subProject(Aligners.Project)
    subProject(UserProjectCompiling.Project)
    subProject(Service.Project)
    subProject(Tests_Windows.Project)
    subProject(XcodeUpdate.Project)
    subProject(UserProjectsK2.Project)
    subProject(Tests_Linux.Project)
    subProject(Tests_MacOS.Project)
})
