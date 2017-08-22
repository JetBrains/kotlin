
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-build-common"))
    compile(project(":compiler:cli-common"))
    compile(project(":kotlin-preloader"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:daemon-common"))
    compile(project(":kotlin-daemon-client"))
    compile(project(":compiler:util"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

