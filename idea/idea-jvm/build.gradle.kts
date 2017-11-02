
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
        compileOnly(intellij { include("openapi.jar", "idea.jar") })

        compile(intellijPlugin("junit") { include("idea-junit.jar") })
        compile(intellijPlugin("testng") { include("testng.jar", "testng-plugin.jar") })
        compile(intellijPlugin("coverage") { include("coverage.jar") })
        compile(intellijPlugin("java-decompiler") { include("java-decompiler.jar") })
    }
}


sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

configureInstrumentation()
