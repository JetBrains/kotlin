
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":core"))
    compile(project(":core::util.runtime"))
    compile(project(":compiler:util"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":compiler:cli"))
    compile(project(":kotlin-test:kotlin-test-jvm"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
