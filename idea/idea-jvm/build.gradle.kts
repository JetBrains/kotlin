
apply { plugin("kotlin") }

dependencies {
    compile(project(":idea"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:frontend.java"))
    compileOnly(intellijDep()) { includeJars("annotations", "openapi", "idea", "extensions", "util", "velocity", "boot", "gson-2.5",
                                             "swingx-core-1.6.2", "forms_rt", "jdom", "log4j", "guava-21.0", "asm-all") }
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
