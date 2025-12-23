plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:cli-base"))

    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
}
