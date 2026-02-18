plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:cli-base"))
    api(project(":compiler:javac-wrapper"))

    compileOnly(toolsJarApi())
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

optInToExperimentalCompilerApi()
optInToK1Deprecation()
