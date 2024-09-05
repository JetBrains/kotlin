package Service.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinScript
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object WindowsVMProvider : BuildType({
    name = "🪟 Windows VM"

    params {
        password("admin.password", "credentialsJSON:540c803b-c4e6-4c83-b3d7-8c23970f5572", description = "Administrator password for the VM. Should be complex enough! Length >=10, special symbols, digits, lower case and upper case.", display = ParameterDisplay.PROMPT)
        checkbox("agent.setup.build.kotlin", "false", description = "Prebuild kotlin project", display = ParameterDisplay.PROMPT,
                  checked = "true", unchecked = "false")
        checkbox("agent.setup.download.intellij", "false", description = "Download intellij community", display = ParameterDisplay.PROMPT,
                  checked = "true", unchecked = "false")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
    }

    steps {
        script {
            name = "Change password and open RDP"
            scriptContent = """
                net user Administrator "%admin.password%" || exit /b 1
                
                netsh advfirewall firewall set rule name="Block all tcp except SSH" new enable=no || exit /b 1
                netsh advfirewall firewall set rule name="Block all udp" new enable=no || exit /b 1
            """.trimIndent()
            formatStderrAsError = true
        }
        gradle {
            name = "Pre-build project"

            conditions {
                equals("agent.setup.build.kotlin", "true")
            }
            tasks = "classes testClasses"
            workingDir = "kotlin"
        }
        script {
            name = "Download IDEA"

            conditions {
                equals("agent.setup.download.intellij", "true")
            }
            scriptContent = "curl https://download.jetbrains.com/idea/ideaIC-2023.1.1.exe -f -L --output ideaIC-2023.1.1.exe"
            formatStderrAsError = true
        }
        kotlinScript {
            name = "Hold machine for 8 hours"
            content = """
                #!/usr/bin/env kotlin
                println("##teamcity[buildStatus text='Ready for connection...']")
                Thread.sleep(8 * 60 * 60 * 1000)
            """.trimIndent()
        }
    }

    failureConditions {
        executionTimeoutMin = 480
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Windows")
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})
