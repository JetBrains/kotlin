
apply { plugin("kotlin") }

configureIntellijPlugin {
    setPlugins("junit", "testng", "coverage", "java-decompiler")
}

dependencies {
    compile(project(":idea"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:frontend.java"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellij { include("openapi.jar", "idea.jar", "extensions.jar", "util.jar") })

        compileOnly(intellijPlugin("junit") { include("idea-junit.jar") })
        compileOnly(intellijPlugin("testng") { include("testng.jar", "testng-plugin.jar") })
        compileOnly(intellijPlugin("coverage") { include("coverage.jar") })
        compileOnly(intellijPlugin("java-decompiler") { include("java-decompiler.jar") })
    }
}


sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

configureInstrumentation()
