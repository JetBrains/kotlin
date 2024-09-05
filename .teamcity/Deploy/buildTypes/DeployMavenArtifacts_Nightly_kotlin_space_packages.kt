package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object DeployMavenArtifacts_Nightly_kotlin_space_packages : BuildType({
    name = "🐧 Maven Artifacts to kotlin.space (kotlin dev) (NIGHTLY)"
    description = "Automatically deploys last successful build to kotlin.space (kotlin dev)"

    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        text("DeployVersion", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("reverse.dep.*.deploy-url", "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        param("reverse.dep.*.deploy-repo", "kotlin-space-packages")
    }

    steps {
        script {
            name = "Trigger kotlin user project compilation on private TeamCity server"
            scriptContent = """
                curl -X POST https://buildserver.labs.intellij.net/app/rest/buildQueue \
                -H "Authorization: Bearer %teamcity.serviceUser.token%" \
                -H "Content-Type: application/xml" \
                -H "Origin: https://buildserver.labs.intellij.net" \
                -d '
                <build>
                    <buildType id="Kotlin_BuildPlayground_Aquarius_Aggregate_user_projects_Nightly"/>
                    <properties>
                        <property name="reverse.dep.*.kotlin_snapshot_version" value="%DeployVersion%"/>
                    </properties>
                </build>'
                
                curl -X PUT https://buildserver.labs.intellij.net/app/rest/buildTypes/Kotlin_BuildPlayground_Aquarius_Aggregate_user_projects_Nightly/parameters/reverse.dep.*.kotlin_snapshot_version \
                -H "Authorization: Bearer %teamcity-jetbrains-com.token%" \
                -H "Origin: https://buildserver.labs.intellij.net" \
                -H "Content-Type: text/plain" \
                -d %DeployVersion%
            """.trimIndent()
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
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(DeployKotlinMavenArtifacts) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(KotlinNativePublishMaven) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        contains("system.cloud.profile_id", "-deployment")
        startsWith("system.cloud.profile_id", "aquarius")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})
