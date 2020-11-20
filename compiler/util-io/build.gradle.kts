plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Kotlin/Native utils"

dependencies {
    compile(kotlinStdlib())
    testImplementation(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

publish()

standardPublicJars()

