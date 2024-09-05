package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object SafeMergeAggregate : BuildType({
    name = "Safe-Merge Aggregate"
    description = "Composite build for Safe-Merge, started by Safe-Merge Coordinator"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")
        root(_Self.vcsRoots.IntellijMonorepoForKotlinVCS_kt_master, "+:. => intellij-kt-master")

        checkoutMode = CheckoutMode.MANUAL
    }

    triggers {
        vcs {
            enabled = false
            branchFilter = "+:<default>"
        }
    }

    features {
        commitStatusPublisher {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            publisher = space {
                authType = connection {
                    connectionId = "PROJECT_EXT_2845"
                }
                projectKey = "KT"
            }
        }
        commitStatusPublisher {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            publisher = space {
                authType = connection {
                    connectionId = "PROJECT_EXT_3432"
                }
                projectKey = "KOTLIN"
            }
        }
    }

    dependencies {
        snapshot(Tests_Linux.buildTypes.CodebaseTests) {
        }
        snapshot(Tests_Linux.buildTypes.GenerateTests_LINUX) {
        }
        snapshot(ResolveDependencies) {
        }
        snapshot(AbsoluteId("ijplatform_master_KotlinCompileIncLatestCompiler_master")) {
        }
    }
})
