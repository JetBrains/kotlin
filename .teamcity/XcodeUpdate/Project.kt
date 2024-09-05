package XcodeUpdate

import XcodeUpdate.buildTypes.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.kubernetesCloudImage
import jetbrains.buildServer.configs.kotlin.kubernetesCloudProfile

object Project : Project({
    id("XcodeUpdate")
    name = "Xcode update helpers"
    description = "Set of helpers to test and update Xcode in Kotlin toolchain"

    buildType(UploadXcodeForKonanToolchain)
    buildType(GradleIntegrationTestsLatestXcode_MACOS)
    buildType(CustomXcodeAggregate)

    features {
        kubernetesCloudImage {
            id = "PROJECT_EXT_1"
            profileId = "aquarius-upload-xcode"
            agentPoolId = "-2"
            agentNamePrefix = "aquarius-upload-xcode"
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
                          image: mac-registry.eqx.k8s.intellij.net/kotlin/kmm-macos-arm64-agent:latest
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
        kubernetesCloudProfile {
            id = "aquarius-upload-xcode"
            name = "aquarius-upload-xcode"
            terminateAfterBuild = true
            terminateIdleMinutes = 30
            apiServerURL = "https://eqx.k8s.intellij.net:6443"
            namespace = "agents-kotlin"
            authStrategy = token {
                token = "credentialsJSON:3f7bc0d8-6183-4618-8587-8956091bd49c"
            }
        }
    }
})
