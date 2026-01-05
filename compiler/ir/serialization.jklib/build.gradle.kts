plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:ir.serialization.jvm"))
    implementation(project(":compiler:cli-base"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
}

optInToUnsafeDuringIrConstructionAPI()