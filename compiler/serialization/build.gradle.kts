
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":core:descriptors"))
    compile(project(":core:deserialization"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
