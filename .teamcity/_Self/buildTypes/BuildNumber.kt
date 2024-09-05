package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*

object BuildNumber : BuildType({
    name = "Build number"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "%chain.build.number%"

    params {
        param("chain.build.number", "%build.number.prefix%-%build.counter%")
        text("deployVersion", "%chain.build.number%", description = "please, change this parameter only in case of release final artifacts deployment", display = ParameterDisplay.PROMPT)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.MANUAL
        cleanCheckout = true
    }
})
