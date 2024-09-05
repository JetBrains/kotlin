package UserProjectsK2

import UserProjectsK2.buildTypes.*
import UserProjectsK2.vcsRoots.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.amazonEC2CloudImage
import jetbrains.buildServer.configs.kotlin.amazonEC2CloudProfile
import jetbrains.buildServer.configs.kotlin.kubernetesCloudImage
import jetbrains.buildServer.configs.kotlin.kubernetesCloudProfile

object Project : Project({
    id("UserProjectsK2")
    name = "K2 User Projects"
    description = """
        Internal user projects with K2 kotlin compiler.
        For external projects visit https://kotlinlang.teamcity.com/project/Kotlin_KotlinCloud_UserProjects
    """.trimIndent()

    vcsRoot(KotlinWasmNodeJSTemplatesK2VCS)
    vcsRoot(KtorForK2VCS)
    vcsRoot(DokkaK2VCS)
    vcsRoot(KotlinWasmComposeTemplatesK2VCS)
    vcsRoot(YouTrackK2Vcs)
    vcsRoot(KotlinWasmBrowserTemplatesK2VCS)
    vcsRoot(KotlinxBenchmarkK2VCS)
    vcsRoot(KotlinWasmWasiTemplatesK2VCS)
    vcsRoot(KotlinxAtomicFuK2VCS)
    vcsRoot(KotlinxDateTimeK2VCS)
    vcsRoot(KotlinxCollectionsImmutableK2VCS)
    vcsRoot(SpaceK2VCS)
    vcsRoot(IntellijK2VCS)

    buildType(ToolboxEnterpriseK2)
    buildType(KotlinxCollectionsImmutableK2)
    buildType(SpaceK2)
    buildType(ExposedK2)
    buildType(KotlinxCoroutinesK2)
    buildType(KotlinWasmTemplatesK2Aggregate)
    buildType(KtorK2)
    buildType(SpaceAndroidK2)
    buildType(KotlinxDateTimeK2)
    buildType(KotlinxBenchmarkK2)
    buildType(SpaceiOSK2)
    buildType(IntellijK2)
    buildType(UserProjectsAggregateK2)
    buildType(DokkaK2)
    buildType(KotlinxAtomicFuK2)
    buildType(YouTrackK2)

    features {
        kubernetesCloudImage {
            id = "PROJECT_EXT_1"
            profileId = "aquarius-up-k8s"
            agentPoolId = "-2"
            agentNamePrefix = "aquarius-up-k8s-up-macos-4cpu-8gb"
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
                          image: mac-registry.eqx.k8s.intellij.net/kotlin-userprojects/userprojects-arm64-agent:xcode-15.3
                          imagePullPolicy: Always
                          resources:
                            requests:
                              cpu: 4
                              memory: 8Gi
                            limits:
                              cpu: 7
                              memory: 8Gi
                          command:
                          - "bash"
                          - "-c"
                          - |
                            /Users/admin/agent-launcher.sh
                """.trimIndent()
            }
        }
        kubernetesCloudImage {
            id = "PROJECT_EXT_2"
            profileId = "aquarius-up-k8s"
            agentPoolId = "-2"
            agentNamePrefix = "aquarius-up-k8s-up-macos-8cpu-16gb"
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
                          image: mac-registry.eqx.k8s.intellij.net/kotlin-userprojects/userprojects-arm64-agent:xcode-15.3
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
            id = "aquarius-up-aws"
            name = "aquarius-up-aws"
            terminateAfterBuild = true
            terminateIdleMinutes = 10
            region = AmazonEC2CloudProfile.Regions.EU_WEST_DUBLIN
            authType = accessKey {
                keyId = "credentialsJSON:2b4ccdbd-40cd-44e9-9146-19f3d9ea75c6"
                secretKey = "credentialsJSON:5bd10e13-b0fa-4b3d-a81c-ac7b53a4dce0"
            }
        }
        amazonEC2CloudImage {
            id = "aquarius-up-aws-linux-4cpu-16gb"
            profileId = "aquarius-up-aws"
            name = "aquarius-up-aws-linux-4cpu-16gb"
            vpcSubnetId = "subnet-034aef4caf2b618bc,subnet-0cb21e7cc090f78a6,subnet-0ffe37cbaedd30e10"
            iamProfile = "PublicBuildAgentInstanceProfile"
            keyPairName = "kotlin-public-tc-agents-2021"
            instanceType = "m6id.xlarge,m5d.xlarge"
            securityGroups = listOf("sg-0aa0cba047e9a1ac9")
            useSpotInstances = true
            instanceTags = mapOf(
                "Name" to "tc.jb.com kotlin",
                "Image" to "aquarius-up-aws-linux-4cpu-16gb",
                "CloudProfileId" to "aquarius-up-aws"
            )
            maxInstancesCount = 50
            source = Source("ami-0c4e591027be9e30f")
        }
        amazonEC2CloudImage {
            id = "aquarius-up-aws-linux-8cpu-32gb"
            profileId = "aquarius-up-aws"
            name = "aquarius-up-aws-linux-8cpu-32gb"
            vpcSubnetId = "subnet-034aef4caf2b618bc,subnet-0cb21e7cc090f78a6,subnet-0ffe37cbaedd30e10"
            iamProfile = "PublicBuildAgentInstanceProfile"
            keyPairName = "kotlin-public-tc-agents-2021"
            instanceType = "m6id.2xlarge,m5d.2xlarge"
            securityGroups = listOf("sg-0aa0cba047e9a1ac9")
            useSpotInstances = true
            instanceTags = mapOf(
                "Name" to "tc.jb.com kotlin",
                "Image" to "aquarius-up-aws-linux-8cpu-32gb",
                "CloudProfileId" to "aquarius-up-aws"
            )
            maxInstancesCount = 50
            source = Source("ami-0c4e591027be9e30f")
        }
        kubernetesCloudProfile {
            id = "aquarius-up-k8s"
            name = "aquarius-up-k8s"
            terminateAfterBuild = true
            terminateIdleMinutes = 30
            apiServerURL = "https://eqx.k8s.intellij.net:6443"
            caCertData = "credentialsJSON:d0854a8d-f28f-4fa4-945a-27fa9c2d4984"
            namespace = "agents-kotlin-userprojects"
            authStrategy = token {
                token = "credentialsJSON:6c6d074d-427d-42b9-b9ae-fb045fa07662"
            }
        }
    }
    buildTypesOrder = arrayListOf(UserProjectsAggregateK2)

    subProject(KotlinxTrainProjectK2.Project)
})
