plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("require-explicit-types")
}

dependencies {

}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}
