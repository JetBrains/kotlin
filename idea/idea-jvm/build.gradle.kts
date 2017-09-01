
apply { plugin("kotlin") }

dependencies {
    compile(project(":idea"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:frontend.java"))

    compileOnly(ideaSdkDeps("openapi", "idea"))

    compile(ideaPluginDeps("idea-junit", plugin = "junit"))
    compile(ideaPluginDeps("testng", "testng-plugin", plugin = "testng"))

    compile(ideaPluginDeps("coverage", plugin = "coverage"))

    compile(ideaPluginDeps("java-decompiler", plugin = "java-decompiler"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
