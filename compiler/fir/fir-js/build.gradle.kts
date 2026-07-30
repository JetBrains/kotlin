plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("require-explicit-types")
    id("power-assert-convention")
}

dependencies {

}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}
