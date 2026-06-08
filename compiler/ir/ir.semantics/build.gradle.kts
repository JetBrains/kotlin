plugins {
    kotlin("jvm")
}

dependencies {}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
