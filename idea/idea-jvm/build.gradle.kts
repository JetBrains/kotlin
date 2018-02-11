
apply { plugin("kotlin") }

dependencies {
    compile(project(":idea"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:frontend.java"))
    compileOnly(intellijDep()) { includeJars("annotations", "openapi", "idea", "extensions", "util", "velocity", "boot", "gson",
                                             "swingx-core", "forms_rt", "jdom", "log4j", "guava", "asm-all", rootProject = rootProject) }
    compileOnly(commonDep("com.google.code.findbugs", "jsr305"))

    compileOnly(intellijPluginDep("junit")) { includeJars("idea-junit") }
    compileOnly(intellijPluginDep("testng")) { includeJars("testng", "testng-plugin") }
    compileOnly(intellijPluginDep("coverage")) { includeJars("coverage") }
    compileOnly(intellijPluginDep("java-decompiler")) { includeJars("java-decompiler") }
    compileOnly(intellijPluginDep("IntelliLang"))
    compileOnly(intellijPluginDep("copyright"))
    compileOnly(intellijPluginDep("properties"))
    compileOnly(intellijPluginDep("java-i18n"))
}


sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

configureInstrumentation()
