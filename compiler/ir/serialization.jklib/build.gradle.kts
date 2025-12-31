plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:ir.psi2ir"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
}

optInToUnsafeDuringIrConstructionAPI()